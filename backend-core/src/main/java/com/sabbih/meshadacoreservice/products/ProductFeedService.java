package com.sabbih.meshadacoreservice.products;

import com.sabbih.meshadacoreservice.ugc.UGCVideo;
import com.sabbih.meshadacoreservice.ugc.UGCVideoRepository;
import com.sabbih.pepperjamservice.models.PProduct;
import com.sabbih.pepperjamservice.models.PepperJamProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProductFeedService {

    private final WebClient pepperClient;
    private final UGCVideoRepository videoRepository;

    @Value("${meshada.pepper.apiKey}")
    private String pepperApiKey;

    @Autowired
    public ProductFeedService(@Qualifier("pepperClient") WebClient pepperClient,
                                  UGCVideoRepository videoRepository) {
        this.pepperClient = pepperClient;
        this.videoRepository = videoRepository;
    }

    public List<String> fetchPepperjamProducts(String programId) {
        List<String> addedProducts = new ArrayList<>();
        try {
            String url = String.format(
                "https://api.pepperjamnetwork.com/20120402/publisher/creative/product?format=json&programIds=%s&apiKey=%s",
                programId, pepperApiKey);

            PepperJamProduct response = pepperClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(PepperJamProduct.class)
                    .block();

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
        } catch (Exception e) {
            log.error("Failed to fetch Pepperjam products: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch products", e);
        }
        return addedProducts;
    }
}
