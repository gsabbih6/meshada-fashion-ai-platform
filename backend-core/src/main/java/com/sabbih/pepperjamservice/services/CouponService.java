package com.sabbih.pepperjamservice.services;

import com.sabbih.pepperjamservice.DModels.Deal;
import com.sabbih.pepperjamservice.DModels.Store;
import com.sabbih.pepperjamservice.models.Coupon;
import com.sabbih.pepperjamservice.models.CouponResponse;
import com.sabbih.pepperjamservice.models.Next;
import com.sabbih.pepperjamservice.utils.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class CouponService {

    private final WebClient webClientPepper;
    private final WebClient orchestratorClient;
    private static final int MAX_RETRIES = 5;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);

    @Value("${meshada.pepper.apiKey}")
    private String pepperKey;

    public CouponService(@Qualifier("orchestrator") WebClient orchestratorClient,
                         @Qualifier("pepperClient") WebClient webClientPepper) {
        this.webClientPepper = webClientPepper;
        this.orchestratorClient = orchestratorClient;
    }

    @Scheduled(cron = "0 0 0 */3 * *")
    private void getDeals() {
        orchestratorClient.get()
                .uri("api/v1/store/all?networkName=pepper")
                .retrieve()
                .bodyToFlux(Store.class)
                .collectList()
                .flatMapMany(Flux::fromIterable)
                .flatMapSequential(store -> processStoreDeals(store)
                        .onErrorResume(e -> {
                            log.error("Error processing store {}: {}", store.getName(), e.getMessage());
                            return Mono.empty(); // Continue with the next store
                        }), 1) // Set concurrency to 1
                .then()
                .subscribe(
//                        () -> log.info("Completed processing all stores"),
//                        error -> log.error("Error during processing: {}", error.getMessage())
                );
    }

    private String buildProductRequestUrl(String code) {
        return Request.builder()
                .format("json")
                .programid(code)
                .apiKey(pepperKey)
                .url("https://api.pepperjamnetwork.com/20120402/publisher/creative/coupon?")
                .build()
                .getProductRequest();
    }

    private Mono<Void> processStoreDeals(Store store) {
        String url = buildProductRequestUrl(store.getCode());
        log.info("Starting to process deals for store '{}'", store.getName());
        return getAllCoupons(url, store)
                .flatMapSequential(couponWithStore -> processCoupon(couponWithStore)
                        .onErrorResume(e -> {
                            log.error("Error processing coupon '{}' for store '{}': {}",
                                    couponWithStore.getCoupon().name, couponWithStore.getStore().getName(), e.getMessage());
                            return Mono.empty(); // Continue with the next coupon
                        }), 1) // Concurrency set to 1
                .doOnComplete(() -> log.info("Completed processing coupons for store '{}'", store.getName()))
                .then();
    }

    private Flux<CouponWithStore> getAllCoupons(String url, Store store) {
        return webClientPepper.get()
                .uri(url)
                .retrieve()
                .bodyToMono(CouponResponse.class)
                .expand(couponResponse -> {
                    Next next = couponResponse.getMeta().getPagination().getNext();
                    if (next != null) {
                        log.info("Fetching next page for store '{}'", store.getName());
                        return webClientPepper.get()
                                .uri(next.getHref())
                                .retrieve()
                                .bodyToMono(CouponResponse.class);
                    } else {
                        return Mono.empty();
                    }
                })
                .flatMapIterable(CouponResponse::getData)
                .map(coupon -> new CouponWithStore(coupon, store))
                .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> log.info("Retrying URL: {} due to error: {}, attempt: {}",
                                url, retrySignal.failure().getMessage(), retrySignal.totalRetries() + 1))
                );
    }

    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            boolean retryable = status >= 500 || status == 429; // Retry for 5xx or 429 errors
            if (!retryable) {
                log.warn("Non-retryable error: {}", ex.toString());
            }
            return retryable;
        }
        return false;
    }

    private Mono<Void> processCoupon(CouponWithStore couponWithStore) {
        Coupon coupon = couponWithStore.getCoupon();
        Store store = couponWithStore.getStore();

        log.info("Processing coupon '{}' for store '{}'", coupon.name, store.getName());

        return saveDeal(coupon, store);
    }

    private Mono<Void> saveDeal(Coupon coupon, Store store) {
        log.info("Saving deal for coupon '{}' under store '{}'", coupon.name, store.getName());

        Deal deal = Deal.builder()
                .name(coupon.name)
                .programName(coupon.programName)
                .endDate(convertStringToInstant(coupon.endDate))
                .startDate(convertStringToInstant(coupon.startDate))
                .exclusive(coupon.exclusive)
                .description(coupon.description)
                .status(coupon.status)
                .code(coupon.code)
                .programId(coupon.programId)
                .coupon(coupon.coupon)
                .categoryName(coupon.categoryName)
                .store(store)
                .build();

        return orchestratorClient.post()
                .uri("api/v1/deals/save")
                .bodyValue(deal)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> log.info("Deal saved successfully for store '{}'", store.getName()))
                .doOnError(e -> log.error("Failed to save deal for store '{}': {}", store.getName(), e.getMessage()))
                .onErrorResume(e -> Mono.empty()); // Continue processing other coupons
    }

    public Instant convertStringToInstant(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty() || dateTimeStr.contains("0000-00-00")) {
            dateTimeStr = "2300-01-01 00:00:00";
            log.warn("Invalid date string provided. Using default date: {}", dateTimeStr);
        }

        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
            ZoneId zoneId = ZoneId.systemDefault();
            return localDateTime.atZone(zoneId).toInstant();
        } catch (Exception e) {
            log.error("Failed to parse date string '{}'. Using default date. Error: {}", dateTimeStr, e.getMessage());
            return LocalDateTime.of(2300, 1, 1, 0, 0).atZone(ZoneId.systemDefault()).toInstant();
        }
    }

    // Helper class to pair Coupon with Store
    private static class CouponWithStore {
        private final Coupon coupon;
        private final Store store;

        public CouponWithStore(Coupon coupon, Store store) {
            this.coupon = coupon;
            this.store = store;
        }

        public Coupon getCoupon() {
            return coupon;
        }

        public Store getStore() {
            return store;
        }
    }
}
