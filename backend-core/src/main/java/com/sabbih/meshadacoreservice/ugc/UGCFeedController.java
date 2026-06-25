package com.sabbih.meshadacoreservice.ugc;

import com.sabbih.meshadacoreservice.social.SocialPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ugc")
public class UGCFeedController {

    private final UGCVideoRepository videoRepository;
    private final UGCEngineService ugcEngineService;
    private final UGCAutoSchedulerService schedulerService;
    private final SocialPublisherService socialPublisherService;

    @Autowired
    public UGCFeedController(UGCVideoRepository videoRepository, 
                             UGCEngineService ugcEngineService, 
                             UGCAutoSchedulerService schedulerService,
                             SocialPublisherService socialPublisherService) {
        this.videoRepository = videoRepository;
        this.ugcEngineService = ugcEngineService;
        this.schedulerService = schedulerService;
        this.socialPublisherService = socialPublisherService;
    }

    @GetMapping("/feed")
    public ResponseEntity<List<UGCVideo>> getFeed() {
        return ResponseEntity.ok(videoRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateVideo(@RequestBody Map<String, String> payload) {
        String productId = payload.getOrDefault("productId", "test_id");
        String productName = payload.getOrDefault("productName", "Test Product");
        String productDescription = payload.getOrDefault("productDescription", "Test Description");
        String productImageUrl = payload.getOrDefault("productImageUrl", "https://example.com/test.jpg");
        String productType = payload.getOrDefault("productType", "test");
        String affiliateLink = payload.getOrDefault("affiliateLink", "https://example.com/product");

        // Run async or block? We'll run in a new thread for now so we don't block the HTTP request
        new Thread(() -> {
            ugcEngineService.generateUGCForProduct(productId, productName, productDescription, productImageUrl, productType, affiliateLink);
        }).start();

        return ResponseEntity.ok("Video generation started in the background.");
    }

    @PostMapping("/scheduler/trigger")
    public ResponseEntity<String> triggerScheduler() {
        new Thread(() -> {
            schedulerService.runDailyUGCPost();
        }).start();
        return ResponseEntity.ok("UGC posting scheduler triggered manually in the background.");
    }

    @PostMapping("/post/{id}")
    public ResponseEntity<String> postVideoManually(@PathVariable Long id) {
        java.util.Optional<UGCVideo> videoOpt = videoRepository.findById(id);
        if (videoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        new Thread(() -> {
            socialPublisherService.publishVideoToSocial(videoOpt.get());
        }).start();
        return ResponseEntity.ok("UGC Video publication started in the background for ID: " + id);
    }
}
