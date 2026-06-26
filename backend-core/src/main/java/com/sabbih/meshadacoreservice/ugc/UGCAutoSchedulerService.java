package com.sabbih.meshadacoreservice.ugc;

import com.sabbih.meshadacoreservice.products.ProductFeedService;
import com.sabbih.meshadacoreservice.social.SocialPublisherService;
import com.sabbih.pepperjamservice.DModels.Product;
import com.sabbih.pepperjamservice.repositories.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class UGCAutoSchedulerService {

    private final UGCVideoRepository videoRepository;
    private final UGCEngineService ugcEngineService;
    private final SocialPublisherService socialPublisherService;
    private final ProductFeedService productFeedService;
    private final ProductRepository productRepository;
    private final Random random = new Random();

    @Autowired
    public UGCAutoSchedulerService(UGCVideoRepository videoRepository,
                                   UGCEngineService ugcEngineService,
                                   SocialPublisherService socialPublisherService,
                                   ProductFeedService productFeedService,
                                   ProductRepository productRepository) {
        this.videoRepository = videoRepository;
        this.ugcEngineService = ugcEngineService;
        this.socialPublisherService = socialPublisherService;
        this.productFeedService = productFeedService;
        this.productRepository = productRepository;
    }

    /**
     * Scheduled task to post videos twice per day.
     * Default: 11:00 AM and 7:00 PM (optimal engagement intervals).
     */
    @Scheduled(cron = "${meshada.ugc.scheduler.cron:0 0 11,19 * * *}")
    public void runDailyUGCPost() {
        log.info("[UGC Scheduler] Starting scheduled daily UGC generation and posting task...");

        // 1. Check if there are generated videos that have not been published yet
        List<UGCVideo> unpublishedVideos = videoRepository.findUnpublishedVideos();
        if (unpublishedVideos != null && !unpublishedVideos.isEmpty()) {
            UGCVideo videoToPublish = unpublishedVideos.get(0);
            log.info("[UGC Scheduler] Found unpublished video in database: {} (ID: {}). Posting it directly without generating a new one.", 
                    videoToPublish.getItemName(), videoToPublish.getId());
            socialPublisherService.publishVideoToSocial(videoToPublish);
            return;
        }

        // 1.5 Auto-fetch new products from Pepperjam so we have real affiliate links
        try {
            log.info("[UGC Scheduler] Fetching latest products from Pepperjam before generating video...");
            productFeedService.fetchPepperjamProducts("7942");
        } catch (Exception e) {
            log.warn("[UGC Scheduler] Failed to fetch latest products from Pepperjam, proceeding with existing database items.", e);
        }

        // 2. Fetch all products from products table that do not have generated videos yet
        List<Product> products = productRepository.findByVideoGeneratedFalse();

        if (products.isEmpty()) {
            log.warn("[UGC Scheduler] No new unused products found in the database. Falling back to an old video.");
            postOldVideoFallback();
            return;
        }

        // 2. Select a random product to generate
        Product product = products.get(random.nextInt(products.size()));
        log.info("[UGC Scheduler] Selected product: {} (ID: {}) for daily generation.", 
                product.getProductName(), product.getId());

        // Extract parameters for generator
        Long productId = product.getId();
        String productName = product.getProductName();
        String productDescription = product.getProductDetails() != null && !product.getProductDetails().isEmpty() 
                ? product.getProductDetails() 
                : "Official styled look for " + productName;
        String productImageUrl = product.getThumbnail();
        String productType = product.getCategory() != null && !product.getCategory().isEmpty() 
                ? product.getCategory() 
                : "fashion";
        String affiliateLink = product.getPaymentUrl();

        // 3. Attempt to generate new video
        log.info("[UGC Scheduler] Triggering UGC video generation for product ID: {}", productId);
        boolean success = ugcEngineService.generateUGCForProduct(
                productId, productName, productDescription, productImageUrl, productType, affiliateLink
        );

        if (success) {
            log.info("[UGC Scheduler] Successfully generated and published new UGC video for: {}", productName);
            // Mark product as video generated
            product.setVideoGenerated(true);
            productRepository.save(product);
        } else {
            // 4. Fallback to old video if credits are exhausted / generation fails
            log.warn("[UGC Scheduler] UGC video generation failed (likely credit exhaustion). Falling back to reposting an old video.");
            postOldVideoFallback();
        }
    }

    /**
     * Fallback logic: select a random completed AI video from database and repost it.
     */
    public void postOldVideoFallback() {
        log.info("[UGC Scheduler] Retrieving historical generated videos for fallback reposting...");
        List<UGCVideo> oldVideos = videoRepository.findGeneratedVideos();

        if (oldVideos.isEmpty()) {
            log.error("[UGC Scheduler] Reposting failed: No previously generated videos found in the database.");
            return;
        }

        // Pick a random historical video
        UGCVideo oldVideo = oldVideos.get(random.nextInt(oldVideos.size()));
        log.info("[UGC Scheduler] Reposting old video: {} (ID: {}, URL: {}) to social media.", 
                oldVideo.getItemName(), oldVideo.getId(), oldVideo.getUrl());

        // Trigger social publisher
        socialPublisherService.publishVideoToSocial(oldVideo);
    }
}
