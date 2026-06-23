package com.sabbih.meshadacoreservice.ugc;

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

    @Autowired
    public UGCFeedController(UGCVideoRepository videoRepository, UGCEngineService ugcEngineService) {
        this.videoRepository = videoRepository;
        this.ugcEngineService = ugcEngineService;
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
}
