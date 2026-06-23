// WebClientConfig.java
package com.sabbih.meshadaaiservices.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

  @Value("${meshada.constants.microservice.endpoint.ai_service}")
  private String aiServiceEndpoint;

  @Value("${meshada.constants.microservice.endpoint.core_service}")
  private String coreServiceEndpoint;

  @Bean("aiService")
  public WebClient aiWebClient() {
    HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(60))
            .compress(true); // Enable compression if supported

    return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(aiServiceEndpoint)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(16 * 1024 * 1024)) // 16 MB buffer
                    .build())
            .build();
  }

  @Bean("orchestrator")
  public WebClient coreServiceWebClient() {
    HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(60))
            .compress(true); // Enable compression if supported

    return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(coreServiceEndpoint)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
  }


}
