package com.sabbih.pepperjamservice.services;

import com.sabbih.pepperjamservice.DModels.Product;
import com.sabbih.pepperjamservice.DModels.Store;
import com.sabbih.pepperjamservice.models.PProduct;
import com.sabbih.pepperjamservice.models.PepperJamProduct;
import com.sabbih.pepperjamservice.utils.ImageUtils;
import com.sabbih.pepperjamservice.utils.Request;
import com.sabbih.pepperjamservice.utils.StylConstants;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class PepperProductService {

    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(1);
    private static final int MAX_RETRIES = 2;
    private static final int CONCURRENCY_LIMIT = 2;
    private static final int BATCH_SIZE = 10;

    private final WebClient webClientPepper;
    private final WebClient orchestratorClient;

    @Value("${meshada.pepper.apiKey}")
    private String pepperKey;

    public PepperProductService(@Qualifier("orchestrator") WebClient orchestratorClient,
                                @Qualifier("pepperClient") WebClient webClientPepper) {
        this.orchestratorClient = orchestratorClient;
        this.webClientPepper = webClientPepper;
    }

//    @Scheduled(cron = "0 0 0 * * ?")
    @Scheduled(fixedDelay = 9000000)
    public void processStoreProducts() {
        orchestratorClient.get()
                .uri("api/v1/store/all?networkName=pepper")
                .retrieve()
                .bodyToFlux(Store.class)
                .collectList()
                .flatMapMany(stores -> {

                    List<Store> filteredStores = stores.stream()
                            .filter(
                                    store ->
                                            !store.getCode().equals("5030"))
                            .toList();

                    return Flux.fromIterable(filteredStores)
                            .flatMap(store -> {
                                String requestUrl = buildProductRequestUrl(store.getCode());
                                log.info("Requesting products for URL: {}", requestUrl);
                                return getPepperJamProduct(requestUrl)
                                        .doOnSuccess(result -> log.info("Processing completed for store: {}", store.getCode()))
                                        .onErrorResume(error -> {
                                            log.error("Error processing store {}: {}", store.getCode(), error.getMessage());
                                            return Mono.empty();
                                        });
                            }, CONCURRENCY_LIMIT);
                })
                .doOnError(error -> log.error("Failed to retrieve stores: {}", error.getMessage()))
                .subscribe();
    }

    private Mono<Void> getPepperJamProduct(String url) {
        return webClientPepper.get()
                .uri(url)
                .retrieve()
                .bodyToMono(PepperJamProduct.class)
                .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                        .filter(throwable -> {
                            if (throwable instanceof WebClientResponseException ex) {
                                return ex.getStatusCode().is5xxServerError();
                            }
                            return false;
                        })
                        .doBeforeRetry(retrySignal -> log.warn("Retrying request for URL: {} after failure", url)))
                .expand(product -> {
                    if (product.getMeta().getPagination().getNext() != null) {
                        String nextUrl = product.getMeta().getPagination().getNext().getHref();
                        log.info("Fetching next page: {}", nextUrl);
                        return webClientPepper.get()
                                .uri(nextUrl)
                                .retrieve()
                                .bodyToMono(PepperJamProduct.class)
                                .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                                        .filter(throwable -> {
                                            if (throwable instanceof WebClientResponseException ex) {
                                                return ex.getStatusCode().is5xxServerError();
                                            }
                                            return false;
                                        })
                                        .doBeforeRetry(retrySignal -> log.warn("Retrying request for URL: {} after failure", nextUrl)));
                    } else {
                        return Mono.empty();
                    }
                })
                .concatMap(product -> Flux.fromIterable(product.getData())
                        .flatMap(this::processProduct, CONCURRENCY_LIMIT)
                        .onErrorContinue((throwable, obj) -> {
                            log.error("Error processing product: {}", throwable.getMessage());
                        })
                )
                .buffer(BATCH_SIZE)
                .flatMap(this::saveProductsBatch)
                .then();
    }

    private Mono<Product> processProduct(PProduct product) {
        return Mono.fromCallable(() -> ImageUtils.isRealImage(product.getImageUrl()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(isReal -> {
                    if (isReal) {
                        return generateSlug(product.getProgramName() + " " + product.getName())
                                .flatMap(slug -> fetchCashback(UUID.nameUUIDFromBytes(product.getProgramName().toLowerCase().getBytes()).toString())
                                        .map(cashback -> buildProduct(product, cashback, slug))
                                );
                    } else {
                        return Mono.empty();
                    }
                });
    }

    private Mono<Void> saveProductsBatch(List<Product> products) {
        log.info("HI: " + products.get(0));
        return orchestratorClient.post()
                .uri("api/product/create/batch")
                .bodyValue(products)
                .retrieve()
                .toEntity(String.class)
                .doOnSuccess(response -> log.info("Batch of {} products saved", products.size()))
                .doOnError(error -> log.error("Failed to save batch of products: {}", error.getMessage()))
                .then();
    }

    private Product buildProduct(PProduct item, Double cashback, String slug) {
        String programName = cleanString(item.getProgramName());
        String productName = cleanString(removeSizePatterns(item.getName()));
        String brandName = cleanString(item.getManufacturer().isEmpty() ? item.getProgramName() : item.getManufacturer());
        Set<String> images = Set.of(Base64.getUrlEncoder().encodeToString(item.getImageUrl().getBytes()));


        return Product.builder()
                .programName(programName)
                .productName(productName)
                .available(item.getInStock().equalsIgnoreCase("Yes") ? "on sale" : "out of stock")
                .productDetails(item.getDescriptionLong())
                .currency(item.getCurrency())
                .paymentUrl(item.getBuyUrl())
                .imageUrls(images)
                .SKU(item.getSku())
                .shippingPrice(StringUtils.isEmpty(item.getPriceShipping()) ?
                        "0.0" :
                        item.getPriceShipping())
                .cashback(
                        cashback * Double.parseDouble(item.getPrice()))
                .thumbnail(images.iterator().next())
                .storeName(item.getProgramName().toLowerCase().split("\\.")[0])
                .brandName(brandName)
                .affiliateName(StylConstants.NETWORKNAME_PEPPERJAM)
                .color(item.getColor().toLowerCase())
                .price(Double.parseDouble(item.getPrice()))
                .priceOld(Double.parseDouble(item.getPrice()))
                .uuidIdentifier(UUID.nameUUIDFromBytes(slug.getBytes()).toString())
                .slug(slug)
                .createdAt(Instant.now())
                .updateAt(Instant.now())
                .build();
    }

    private Mono<Double> fetchCashback(String unique) {
        return orchestratorClient.get()
                .uri("api/v1/store/cashback/" + unique)
                .retrieve()
                .bodyToMono(Double.class)
                .onErrorReturn(0.0);
    }

    private String cleanString(String input) {
        return input.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "").trim();
    }

    private String removeSizePatterns(String productName) {
        String sizePattern = "(?i)\\b(2x|3x|4x|XxxSmall|XxSmall|XSmall|Small|XxxLarge|XxLarge|XLarge|Large|Medium|XXS|XS|S|M|L|XL|XXL|XXXL)\\b";
        return productName.replaceAll(sizePattern, "").replaceAll("\\s{2,}", " ").trim();
    }

    private String buildProductRequestUrl(String programIds) {
        return Request.builder()
                .format("json")
                .programid(programIds)
                .apiKey(pepperKey)
                .url("https://api.pepperjamnetwork.com/20120402/publisher/creative/product?")
                .build()
                .getProductRequest();
    }

    public Mono<String> generateSlug(String productName) {
        String initialSlug = cleanString(productName).replaceAll("\\s+", "-");
        log.info("Generating slug for product name: {}", productName);
        return Mono.just(initialSlug);
    }
}
