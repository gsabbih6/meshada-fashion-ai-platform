package com.sabbih.pepperjamservice.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfigs {
    @Value("${meshada.constants.microservice.endpoint.core_service}")
    private String coreService;

    @Bean("pepperClient")
    public WebClient webClientPepper() {
        ClientHttpConnector connector = new ReactorClientHttpConnector(

        );
        final int size = 16 * 1024 * 1024;
        final ExchangeStrategies strategies =
                ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                        .build();
        return WebClient.builder()
                .clientConnector(connector)
                .exchangeStrategies(strategies)
                //        .baseUrl("https://api.pepperjamnetwork.com/20120402/")
                .build();
    }
    @Bean("orchestrator")
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(coreService)
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs(codecs -> codecs
                                .defaultCodecs()
                                .maxInMemorySize(500 * 1024))
                        .build())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
