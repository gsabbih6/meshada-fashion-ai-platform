package com.sabbih.meshadacoreservice.ugc;
 
import com.sabbih.meshadacoreservice.social.SocialPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
 
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
 
@RestController
@RequestMapping("/api/v1/ugc")
public class UGCFeedController {
 
    @Value("${meshada.app.url:https://www.meshada.com}")
    private String appUrl;
 
    private final UGCVideoRepository videoRepository;
    private final UGCEngineService ugcEngineService;
    private final UGCAutoSchedulerService schedulerService;
    private final SocialPublisherService socialPublisherService;
    private final Executor taskExecutor;
 
    @Autowired
    public UGCFeedController(UGCVideoRepository videoRepository, 
                             UGCEngineService ugcEngineService, 
                             UGCAutoSchedulerService schedulerService,
                             SocialPublisherService socialPublisherService,
                             Executor taskExecutor) {
        this.videoRepository = videoRepository;
        this.ugcEngineService = ugcEngineService;
        this.schedulerService = schedulerService;
        this.socialPublisherService = socialPublisherService;
        this.taskExecutor = taskExecutor;
    }
 
    @GetMapping("/feed")
    public ResponseEntity<List<UGCVideo>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(videoRepository.findAll(pageable).getContent());
    }
 
    @PostMapping("/generate")
    public ResponseEntity<String> generateVideo(@RequestBody Map<String, String> payload) {
        String productId = payload.getOrDefault("productId", "test_id");
        String productName = payload.getOrDefault("productName", "Test Product");
        String productDescription = payload.getOrDefault("productDescription", "Test Description");
        String productImageUrl = payload.getOrDefault("productImageUrl", "https://example.com/test.jpg");
        String productType = payload.getOrDefault("productType", "test");
        String affiliateLink = payload.getOrDefault("affiliateLink", "https://example.com/product");
 
        // Run async using Spring-managed Executor
        taskExecutor.execute(() -> {
            ugcEngineService.generateUGCForProduct(productId, productName, productDescription, productImageUrl, productType, affiliateLink);
        });
 
        return ResponseEntity.ok("Video generation started in the background.");
    }
 
    @PostMapping("/scheduler/trigger")
    public ResponseEntity<String> triggerScheduler() {
        taskExecutor.execute(() -> {
            schedulerService.runDailyUGCPost();
        });
        return ResponseEntity.ok("UGC posting scheduler triggered manually in the background.");
    }
 
    @PostMapping("/post/{id}")
    public ResponseEntity<String> postVideoManually(@PathVariable Long id) {
        java.util.Optional<UGCVideo> videoOpt = videoRepository.findById(id);
        if (videoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        taskExecutor.execute(() -> {
            socialPublisherService.publishVideoToSocial(videoOpt.get());
        });
        return ResponseEntity.ok("UGC Video publication started in the background for ID: " + id);
    }

    @GetMapping("/debug-config")
    public ResponseEntity<Map<String, String>> getDebugConfig() {
        return ResponseEntity.ok(Map.of(
            "MESHADA_APP_URL_env", System.getenv().getOrDefault("MESHADA_APP_URL", "not set"),
            "appUrl_resolved", appUrl
        ));
    }
}
