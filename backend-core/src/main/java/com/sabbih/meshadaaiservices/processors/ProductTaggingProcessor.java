package com.sabbih.meshadaaiservices.processors;

import com.sabbih.meshadaaiservices.model.AiData;
import com.sabbih.meshadaaiservices.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.Serializable;
import java.time.Duration;
import java.util.Properties;

@Slf4j
@Component
public class ProductTaggingProcessor implements Serializable {
    // Limit the number of concurrent requests to the AI service
    private static final int MAX_CONCURRENT_REQUESTS = 10; // Increased from 3 to 10
    private final SparkSession sparkSession;
    private final WebClient aiWebClient;
    private final WebClient coreServiceWebClient;
    @Value("${meshada.constants.microservice.endpoint.img_proxy}")
    private String imgProxyUrl;
    @Value("${datasource.url}")
    private String jdbcUrl;
    @Value("${datasource.username}")
    private String dbUser;
    @Value("${datasource.password}")
    private String dbPassword;

    public ProductTaggingProcessor(SparkSession sparkSession,
                                   @Qualifier("aiService") WebClient aiServiceWebClient,
                                   @Qualifier("orchestrator") WebClient orchestratorWebClient) {
        this.sparkSession = sparkSession;
        this.aiWebClient = aiServiceWebClient;
        this.coreServiceWebClient = orchestratorWebClient;
    }

    //    @Async("blockingTaskExecutor")
//    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000) // every 24 hours
    @Scheduled(cron = "0 0 0 */2 * ?")
    public void processProductTags() {
        Dataset<Row> messages = readProductData();
        if (!messages.isEmpty()) {
            processMessages(messages)
                    .doOnError(e -> log.error("Error processing product tags", e))
                    .doOnTerminate(() -> log.info("Finished processing product tags."))
                    .subscribe(
                            null,
                            error -> log.error("Processing product tags encountered an error: ", error),
                            () -> log.info("Processing product tags completed successfully.")
                    );
        } else {
            log.info("No product data to process.");
        }
    }


    private Dataset<Row> readProductData() {
        Properties connectionProperties = new Properties();
        connectionProperties.put("user", dbUser);
        connectionProperties.put("password", dbPassword);
        connectionProperties.put("fetchsize", "1000");
        connectionProperties.put("driver", "com.mysql.cj.jdbc.Driver");
        connectionProperties.put("ssl", "true");

        return sparkSession
                .read()
                .jdbc(jdbcUrl, "product", connectionProperties)
                .na()
                .fill(0L);
    }

    private Mono<Void> processMessages(Dataset<Row> messages) {
        return Flux.fromIterable(messages.collectAsList())
                .flatMap(this::processRowAsync, MAX_CONCURRENT_REQUESTS)
                .onErrorContinue((throwable, o) -> {
                    log.error("Error processing messages: {}", throwable.getMessage());
                })
                .then();
    }

    private Mono<Void> processRowAsync(Row row) {
        return fetchAiDataAsync(row)
                .flatMap(aiData -> postProcessedDataAsync(row, aiData))
                .onErrorResume(e -> {
                    log.error("Error processing row {}: {}", row.getAs("id"), e.getMessage());
                    return Mono.empty(); // Continue processing other rows
                });
    }

    private Mono<AiData> fetchAiDataAsync(Row row) {
//        String thumbnail = row.getAs("thumbnail");
//        String productDetails = row.getAs("product_details");
//        String rowId = row.getAs("id").toString();
//
//        if (thumbnail == null || productDetails == null) {
//            log.warn("Skipping row {} due to missing thumbnail or product details.", rowId);
//            return Mono.empty();
//        }

//        String imageUrl = String.format("%s/sig/resize:auto:150:150:true:true/%s.webp", imgProxyUrl, thumbnail);
//
//        log.info("Processing row ID: {}, Image URL: {}", rowId, imageUrl);

        return aiWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/product/category")
                        .queryParam("url", String.format("%s/sig/resize:auto:350:500:true:true/%s.webp", imgProxyUrl, row.getAs("thumbnail")))
                        .queryParam("desc", row.getAs("product_details").toString())
                        .build())
                .retrieve()
                .bodyToMono(AiData.class)
                .doOnNext(aiData -> log.info("AI Data received for row {}: {}", row.getAs("id").toString(), aiData))
                .timeout(Duration.ofSeconds(120))
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .filter(throwable -> {
                            log.warn("Retrying fetchAiDataAsync for row {} due to: {}", row.getAs("id").toString(), throwable.getMessage());
                            return true;
                        }))
                .doOnError(e -> log.error("Failed to fetch AI data for row {}: {}", row.getAs("id").toString(), e.getMessage()))
                .onErrorResume(e -> Mono.empty()); // Continue processing other rows
    }

    private Mono<Void> postProcessedDataAsync(Row row, AiData aiData) {
//        if (aiData == null) {
//            log.warn("AI Data is null for row {}", row.getAs("id").toString());
//            return Mono.empty();
//        }

//        String rowId = row.getAs("id").toString();

        return Mono.fromCallable(() -> JsonUtils.serialize(aiData))
                .flatMap(serializedData -> {
                    log.info("Serialized AI Data for row {}: {}", row.getAs("id").toString(), serializedData);
                    return coreServiceWebClient
                            .post()
                            .uri("/api/product/tags/" + row.getAs("id").toString())
                            .bodyValue(aiData)
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(120))
                            .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
                                    .maxBackoff(Duration.ofSeconds(30))
                                    .filter(throwable -> {
                                        log.warn("Retrying postProcessedDataAsync for row {} due to: {}", row.getAs("id").toString(), throwable.getMessage());
                                        return true;
                                    }))
                            .doOnNext(res -> log.info("Successfully posted tags for row {}: {}", row.getAs("id").toString(), res))
                            .doOnError(e -> log.error("Failed to post tags for row {}: {}", row.getAs("id").toString(), e.getMessage()))
                            .onErrorResume(e -> Mono.empty()); // Continue processing other rows
                })
                .onErrorResume(e -> {
                    log.error("Error serializing or posting data for row {}: {}", row.getAs("id").toString(), e.getMessage());
                    return Mono.empty();
                }).then();
    }

//    private void processMessages(Dataset<Row> messages) {
//        // Capture necessary variables that are serializable
//        String aiServiceEndpointLocal = aiServiceEndpoint;
//        String coreServiceEndpointLocal = coreServiceEndpoint;
//        String imgProxyUrlLocal = imgProxyUrl;
//
//        messages.foreachPartition(iterator -> {
//            // Instantiate WebClient instances inside the closure
//            ConnectionProvider provider = ConnectionProvider.builder("custom")
//                    .maxConnections(2000)
//                    .pendingAcquireMaxCount(2000)
//                    .build();
//
//            HttpClient httpClient = HttpClient.create(provider);
//
//            // Configure timeouts
//            HttpClient aiHttpClient = httpClient
//                    .responseTimeout(Duration.ofSeconds(30));
//
//            WebClient aiWebClient = WebClient.builder()
//                    .clientConnector(new ReactorClientHttpConnector(aiHttpClient))
//                    .baseUrl(aiServiceEndpointLocal)
//                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                    .exchangeStrategies(ExchangeStrategies.builder()
//                            .codecs(configurer -> configurer
//                                    .defaultCodecs()
//                                    .maxInMemorySize(16 * 1024 * 1024)) // Increase buffer size if needed
//                            .build())
//                    .build();
//
//            WebClient coreServiceWebClient = WebClient.builder()
//                    .clientConnector(new ReactorClientHttpConnector(httpClient))
//                    .baseUrl(coreServiceEndpointLocal)
//                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                    .build();
//
//            // Process rows sequentially
//            while (iterator.hasNext()) {
//                Row row = iterator.next();
//                try {
//                    processRowSequentially(row, aiWebClient, coreServiceWebClient, imgProxyUrlLocal).block();
//                } catch (Exception e) {
//                    log.error("Error processing row {}: {}", row.getAs("id"), e.getMessage());
//                }
//            }
//        });
//    }

//    private Mono<Void> processRowSequentially(Row row, WebClient aiWebClient, WebClient coreServiceWebClient, String imgProxyUrlLocal) {
//        return fetchAiDataAsync(row, aiWebClient, imgProxyUrlLocal)
//                .flatMap(aiData -> postProcessedDataAsync(row, aiData, coreServiceWebClient))
//                .onErrorResume(e -> {
//                    log.error("Error in processRowSequentially for row {}: {}", row.getAs("id"), e.getMessage());
//                    return Mono.empty();
//                });
//    }

}
