package com.sabbih.meshadacoreservice.products;

import com.sabbih.meshadacoreservice.ugc.UGCVideo;
import com.sabbih.meshadacoreservice.ugc.UGCVideoRepository;
import com.sabbih.pepperjamservice.DModels.Product;
import com.sabbih.pepperjamservice.models.PProduct;
import com.sabbih.pepperjamservice.models.PepperJamProduct;
import com.sabbih.pepperjamservice.repositories.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ProductFeedService {

    private final WebClient pepperClient;
    private final UGCVideoRepository videoRepository;
    private final ProductRepository productRepository;

    @Value("${meshada.pepper.apiKey}")
    private String pepperApiKey;

    @Autowired
    public ProductFeedService(@Qualifier("pepperClient") WebClient pepperClient,
                              UGCVideoRepository videoRepository,
                              ProductRepository productRepository) {
        this.pepperClient = pepperClient;
        this.videoRepository = videoRepository;
        this.productRepository = productRepository;
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

                        if (!productRepository.existsBySKU(product.getSku())) {
                            Product newProduct = Product.builder()
                                    .SKU(product.getSku())
                                    .productName(product.getName())
                                    .programName(product.getProgramName())
                                    .paymentUrl(product.getBuyUrl())
                                    .thumbnail(product.getImageUrl())
                                    .price(Double.parseDouble(product.getPrice()))
                                    .currency(product.getCurrency())
                                    .color(product.getColor())
                                    .brandName(product.getManufacturer())
                                    .videoGenerated(false)
                                    .createdAt(Instant.now())
                                    .updateAt(Instant.now())
                                    .build();

                            productRepository.save(newProduct);
                            addedProducts.add(product.getName());
                            count++;
                        }
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
