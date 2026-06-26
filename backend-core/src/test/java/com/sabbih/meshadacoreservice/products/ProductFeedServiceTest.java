package com.sabbih.meshadacoreservice.products;

import com.sabbih.meshadacoreservice.ugc.UGCVideoRepository;
import com.sabbih.pepperjamservice.DModels.Product;
import com.sabbih.pepperjamservice.models.PProduct;
import com.sabbih.pepperjamservice.models.PepperJamProduct;
import com.sabbih.pepperjamservice.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductFeedServiceTest {

    private WebClient webClient;
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    private WebClient.ResponseSpec responseSpec;

    private UGCVideoRepository videoRepository;
    private ProductRepository productRepository;
    private ProductFeedService productFeedService;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class);
        requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        videoRepository = mock(UGCVideoRepository.class);
        productRepository = mock(ProductRepository.class);

        productFeedService = new ProductFeedService(webClient, videoRepository, productRepository);
    }

    @Test
    void testFetchPepperjamProductsSuccess() {
        PProduct p1 = new PProduct();
        p1.setSku("sku1");
        p1.setName("Tank Top");
        p1.setBuyUrl("https://buy.url");
        p1.setImageUrl("https://image.url");
        p1.setPrice("19.99");
        p1.setProgramName("Royal Apparel");

        PepperJamProduct mockResponse = new PepperJamProduct();
        mockResponse.setData(List.of(p1));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(PepperJamProduct.class)).thenReturn(Mono.just(mockResponse));

        when(productRepository.existsBySKU("sku1")).thenReturn(false);

        List<String> added = productFeedService.fetchPepperjamProducts("7942");

        assertEquals(1, added.size());
        assertEquals("Tank Top", added.get(0));
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testFetchPepperjamProductsAlreadyExists() {
        PProduct p1 = new PProduct();
        p1.setSku("sku-exist");
        p1.setName("Exist Tank Top");
        p1.setBuyUrl("https://buy.url");
        p1.setImageUrl("https://image.url");
        p1.setPrice("19.99");

        PepperJamProduct mockResponse = new PepperJamProduct();
        mockResponse.setData(List.of(p1));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(PepperJamProduct.class)).thenReturn(Mono.just(mockResponse));

        when(productRepository.existsBySKU("sku-exist")).thenReturn(true);

        List<String> added = productFeedService.fetchPepperjamProducts("7942");

        assertEquals(0, added.size());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testFetchPepperjamProductsException() {
        when(webClient.get()).thenThrow(new RuntimeException("API Connection Failed"));

        assertThrows(RuntimeException.class, () -> {
            productFeedService.fetchPepperjamProducts("7942");
        });
    }
}
