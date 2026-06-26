package com.sabbih.meshadacoreservice.ugc;

import com.sabbih.meshadacoreservice.products.ProductFeedService;
import com.sabbih.meshadacoreservice.social.SocialPublisherService;
import com.sabbih.pepperjamservice.DModels.Product;
import com.sabbih.pepperjamservice.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

class UGCAutoSchedulerTests {

    private UGCVideoRepository videoRepository;
    private UGCEngineService ugcEngineService;
    private SocialPublisherService socialPublisherService;
    private ProductFeedService productFeedService;
    private ProductRepository productRepository;
    private UGCAutoSchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        videoRepository = mock(UGCVideoRepository.class);
        ugcEngineService = mock(UGCEngineService.class);
        socialPublisherService = mock(SocialPublisherService.class);
        productFeedService = mock(ProductFeedService.class);
        productRepository = mock(ProductRepository.class);
        schedulerService = new UGCAutoSchedulerService(
                videoRepository, ugcEngineService, socialPublisherService, productFeedService, productRepository
        );
    }

    @Test
    void testSchedulerSuccessfullyGeneratesAndPostsNewVideo() {
        // Arrange
        Product product = Product.builder()
                .id(10L)
                .productName("Cool Tanktop")
                .thumbnail("https://example.com/placeholder.jpg")
                .paymentUrl("https://example.com/shop")
                .videoGenerated(false)
                .build();

        when(productRepository.findByVideoGeneratedFalse()).thenReturn(List.of(product));
        when(ugcEngineService.generateUGCForProduct(
                eq(10L), eq("Cool Tanktop"), anyString(), eq("https://example.com/placeholder.jpg"), anyString(), eq("https://example.com/shop")
        )).thenReturn(true);

        // Act
        schedulerService.runDailyUGCPost();

        // Assert
        verify(ugcEngineService, times(1)).generateUGCForProduct(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(productRepository, times(1)).save(product);
        verify(socialPublisherService, never()).publishVideoToSocial(any(UGCVideo.class)); // Generation handles publishing in service
    }

    @Test
    void testSchedulerFallsBackToOldVideoWhenGenerationFails() {
        // Arrange: 1 product, 1 old video
        Product product = Product.builder()
                .id(10L)
                .productName("Cool Tanktop")
                .thumbnail("https://example.com/placeholder.jpg")
                .paymentUrl("https://example.com/shop")
                .videoGenerated(false)
                .build();

        UGCVideo oldVideo = UGCVideo.builder()
                .id(2L)
                .itemName("Old Jersey Shirt")
                .url("https://example.com/generated_video.mp4")
                .affiliateLink("https://example.com/shop-old")
                .build();

        when(productRepository.findByVideoGeneratedFalse()).thenReturn(List.of(product));
        when(videoRepository.findGeneratedVideos()).thenReturn(List.of(oldVideo));
        
        // Mock generation failure (e.g. credit exhaustion)
        when(ugcEngineService.generateUGCForProduct(
                anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(false);

        // Act
        schedulerService.runDailyUGCPost();

        // Assert
        verify(ugcEngineService, times(1)).generateUGCForProduct(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(videoRepository, times(1)).findGeneratedVideos();
        verify(socialPublisherService, times(1)).publishVideoToSocial(oldVideo); // Fallback triggers old video posting
    }

    @Test
    void testSchedulerFallsBackToOldVideoWhenNoProductsExist() {
        // Arrange: 0 products, 1 old video
        UGCVideo oldVideo = UGCVideo.builder()
                .id(2L)
                .itemName("Old Jersey Shirt")
                .url("https://example.com/generated_video.mp4")
                .affiliateLink("https://example.com/shop-old")
                .build();

        when(productRepository.findByVideoGeneratedFalse()).thenReturn(Collections.emptyList());
        when(videoRepository.findGeneratedVideos()).thenReturn(List.of(oldVideo));

        // Act
        schedulerService.runDailyUGCPost();

        // Assert
        verify(ugcEngineService, never()).generateUGCForProduct(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(videoRepository, times(1)).findGeneratedVideos();
        verify(socialPublisherService, times(1)).publishVideoToSocial(oldVideo);
    }

    @Test
    void testSchedulerPublishesUnpublishedVideoDirectly() {
        // Arrange: 1 unpublished completed video in database
        UGCVideo unpublishedVideo = UGCVideo.builder()
                .id(5L)
                .itemName("Unpublished Silk Dress")
                .url("https://example.com/generated_silk.mp4")
                .affiliateLink("https://example.com/shop-silk")
                .published(false)
                .build();

        when(videoRepository.findUnpublishedVideos()).thenReturn(List.of(unpublishedVideo));

        // Act
        schedulerService.runDailyUGCPost();

        // Assert: Publishes the existing video directly and does NOT trigger new generation
        verify(socialPublisherService, times(1)).publishVideoToSocial(unpublishedVideo);
        verify(ugcEngineService, never()).generateUGCForProduct(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
