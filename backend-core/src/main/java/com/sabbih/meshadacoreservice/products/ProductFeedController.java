package com.sabbih.meshadacoreservice.products;

import com.sabbih.meshadacoreservice.ugc.UGCVideo;
import com.sabbih.meshadacoreservice.ugc.UGCVideoRepository;
import com.sabbih.pepperjamservice.models.PProduct;
import com.sabbih.pepperjamservice.models.PepperJamProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@Slf4j
public class ProductFeedController {

    private final WebClient pepperClient;
    private final UGCVideoRepository videoRepository;

    @Value("${meshada.pepper.apiKey}")
    private String pepperApiKey;

    @Autowired
    public ProductFeedController(@Qualifier("pepperClient") WebClient pepperClient,
                                  UGCVideoRepository videoRepository) {
        this.pepperClient = pepperClient;
        this.videoRepository = videoRepository;
    }

    /**
     * Fetch latest fashion products from Pepperjam and seed them into UGC feed.
     */
    @PostMapping("/fetch-pepperjam")
    public ResponseEntity<Map<String, Object>> fetchPepperjamProducts(
            @RequestParam(defaultValue = "4977") String programId) {

        try {
            String url = String.format(
                "https://api.pepperjamnetwork.com/20120402/publisher/creative/product?format=json&programId=%s&apiKey=%s",
                programId, pepperApiKey);

            PepperJamProduct response = pepperClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(PepperJamProduct.class)
                    .block();

            List<String> addedProducts = new ArrayList<>();

            if (response != null && response.getData() != null) {
                int count = 0;
                for (PProduct product : response.getData()) {
                    if (count >= 10) break; // Limit to 10 products per fetch

                    if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()
                            && product.getBuyUrl() != null && !product.getBuyUrl().isEmpty()) {

                        UGCVideo video = UGCVideo.builder()
                                .url(product.getImageUrl()) // Use product image as placeholder until AI generates video
                                .affiliateLink(product.getBuyUrl())
                                .modelName(product.getProgramName())
                                .itemName(product.getName())
                                .createdAt(LocalDateTime.now())
                                .build();

                        videoRepository.save(video);
                        addedProducts.add(product.getName());
                        count++;
                    }
                }
            }

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
