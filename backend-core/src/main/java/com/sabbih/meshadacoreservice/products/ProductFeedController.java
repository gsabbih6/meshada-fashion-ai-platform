package com.sabbih.meshadacoreservice.products;

import com.sabbih.meshadacoreservice.ugc.UGCVideo;
import com.sabbih.meshadacoreservice.ugc.UGCVideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@Slf4j
public class ProductFeedController {

    private final ProductFeedService productFeedService;
    private final UGCVideoRepository videoRepository;

    @Autowired
    public ProductFeedController(ProductFeedService productFeedService,
                                  UGCVideoRepository videoRepository) {
        this.productFeedService = productFeedService;
        this.videoRepository = videoRepository;
    }

    /**
     * Fetch latest fashion products from Pepperjam and seed them into UGC feed.
     */
    @PostMapping("/fetch-pepperjam")
    public ResponseEntity<Map<String, Object>> fetchPepperjamProducts(
            @RequestParam(defaultValue = "7942") String programId) {

        try {
            List<String> addedProducts = productFeedService.fetchPepperjamProducts(programId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "productsAdded", addedProducts.size(),
                    "products", addedProducts
            ));

        } catch (Exception e) {
            log.error("Failed to fetch Pepperjam products: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * List all products currently in the feed.
     */
    @GetMapping("/list")
    public ResponseEntity<List<UGCVideo>> listProducts() {
        return ResponseEntity.ok(videoRepository.findAllByOrderByCreatedAtDesc());
    }
}
