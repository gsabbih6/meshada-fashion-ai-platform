//package com.sabbih.meshadaaiservices.listeners;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.sabbih.meshadaaiservices.model.AiData;
//import com.sabbih.meshadaaiservices.model.Product;
//import com.sabbih.meshadaaiservices.utils.JsonUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//@Slf4j
//@Component
//public class ProductListener {
//  private final WebClient aiServiceWebClient;
//  private final WebClient orchestratorWebClient;
//
//  @Value("${meshada.constants.microservice.endpoint.img_proxy}")
//  private String imgProxyUrl;
//
//  public ProductListener(
//      @Qualifier("aiService") WebClient aiServiceWebClient,
//      @Qualifier("orchestrator") WebClient orchestratorWebClient) {
//    this.aiServiceWebClient = aiServiceWebClient;
//    this.orchestratorWebClient = orchestratorWebClient;
//  }
//
//  @KafkaListener(groupId = "groupId", topics = "${meshada.constants.kafka.product_topic}")
//  public void tagProcessor(String feed) {
//    try {
//      Product product = JsonUtils.deserialize(feed, Product.class);
//      fetchAiData(product)
//          .subscribe(
//              aiData -> processAiData(aiData, product),
//              error -> log.error("Error fetching AI data: {}", error.getMessage()));
//    } catch (JsonProcessingException e) {
//      log.error("Error deserializing feed: {}", e.getMessage());
//    }
//  }
//
//  private Mono<AiData> fetchAiData(Product product) {
//    return aiServiceWebClient
//        .get()
//        .uri(
//            uriBuilder ->
//                uriBuilder
//                    .path("/product/category")
//                    .queryParam(
//                        "url",
//                        String.format("%s/sig/q:100/%s", imgProxyUrl, product.getThumbnail()))
//                    .queryParam("desc", "a")
//                    .build())
//        .retrieve()
//        .bodyToMono(AiData.class)
//        .doOnError(error -> log.error("Error during AI data retrieval: {}", error.getMessage()));
//  }
//
//  private void processAiData(AiData aiData, Product product) {
//    try {
//      log.info("AI Data: {}", JsonUtils.serialize(aiData));
//      persistAiData(aiData, product.getId());
//    } catch (JsonProcessingException e) {
//      log.error("Error serializing AI data: {}", e.getMessage());
//    }
//  }
//
//  private void persistAiData(AiData aiData, Long productId) {
//    orchestratorWebClient
//        .post()
//        .uri("/api/product/tags/" + productId)
//        .bodyValue(aiData)
//        .retrieve()
//        .bodyToMono(String.class)
//        .doOnSuccess(
//            response -> log.info("Tags persisted for product ID {}: {}", productId, response))
//        .subscribe();
//  }
//}
package com.sabbih.meshadaaiservices.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sabbih.meshadaaiservices.model.AiData;
import com.sabbih.meshadaaiservices.model.Product;
import com.sabbih.meshadaaiservices.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
public class ProductListener {
  private final WebClient aiServiceWebClient;
  private final WebClient orchestratorWebClient;

  @Value("${meshada.constants.microservice.endpoint.img_proxy}")
  private String imgProxyUrl;

  public ProductListener(
          @Qualifier("aiService") WebClient aiServiceWebClient,
          @Qualifier("orchestrator") WebClient orchestratorWebClient) {
    this.aiServiceWebClient = aiServiceWebClient;
    this.orchestratorWebClient = orchestratorWebClient;
  }

//  @KafkaListener(groupId = "groupId", topics = "${meshada.constants.kafka.product_topic}")
  public void tagProcessor(String feed) {
    try {
      Product product = JsonUtils.deserialize(feed, Product.class);
      fetchAiData(product)
              .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(5)))
              .timeout(Duration.ofSeconds(30))
              .subscribe(
                      aiData -> processAiData(aiData, product),
                      error -> log.error("Error fetching AI data: {}", error.getMessage()));
    } catch (JsonProcessingException e) {
      log.error("Error deserializing feed: {}", e.getMessage());
    }
  }

  private Mono<AiData> fetchAiData(Product product) {
    return aiServiceWebClient
            .get()
            .uri(
                    uriBuilder ->
                            uriBuilder
                                    .path("/product/category")
                                    .queryParam(
                                            "url",
                                            String.format("%s/sig/q:100/%s", imgProxyUrl, product.getThumbnail()))
                                    .queryParam("desc", product.getProductDetails())
                                    .build())
            .retrieve()
            .bodyToMono(AiData.class)
            .doOnError(error -> log.error("Error during AI data retrieval: {}", error.getMessage()))
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(5)))
            .timeout(Duration.ofSeconds(30));
  }

  private void processAiData(AiData aiData, Product product) {
    try {
      log.info("AI Data: {}", JsonUtils.serialize(aiData));
      persistAiData(aiData, product.getId());
    } catch (JsonProcessingException e) {
      log.error("Error serializing AI data: {}", e.getMessage());
    }
  }

  private void persistAiData(AiData aiData, Long productId) {
    orchestratorWebClient
            .post()
            .uri("/api/product/tags/" + productId)
            .bodyValue(aiData)
            .retrieve()
            .bodyToMono(String.class)
            .doOnSuccess(
                    response -> log.info("Tags persisted for product ID {}: {}", productId, response))
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(5)))
            .timeout(Duration.ofSeconds(30))
            .subscribe();
  }
}
