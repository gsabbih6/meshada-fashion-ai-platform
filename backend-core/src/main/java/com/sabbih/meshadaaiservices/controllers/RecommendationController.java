package com.sabbih.meshadaaiservices.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sabbih.meshadaaiservices.model.UserRecommendation;
import com.sabbih.meshadaaiservices.service.RecommendationService;
import com.sabbih.meshadaaiservices.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
@ConditionalOnProperty(name = "meshada.spark.enabled", havingValue = "true")
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Autowired
    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{userId}")
    public UserRecommendation getRecommendations(
            @PathVariable int userId,
            @RequestParam(defaultValue = "10") int numRecommendations) {
        try {
            log.info("Fetching {} recommendations for user: {}", numRecommendations, userId);
            Dataset<Row> recommendations = recommendationService.recommendProductsForUser(userId, numRecommendations);
            return convertDatasetToJson(recommendations);
        } catch (Exception e) {
            log.error("Error fetching recommendations for user {}: {}", userId, e.getMessage());
            return new UserRecommendation(); // Return empty response
        }
    }

    @GetMapping("/all")
    public UserRecommendation getRecommendationsForAll(
            @RequestParam(defaultValue = "20") int numRecommendations) {
        try {
            log.info("Fetching {} recommendations for all users", numRecommendations);
            Dataset<Row> recommendations = recommendationService.recommendProductsForAll(numRecommendations);
            return convertDatasetToJson(recommendations);
        } catch (Exception e) {
            log.error("Error fetching recommendations for all users: {}", e.getMessage());
            return new UserRecommendation();
        }
    }

    private UserRecommendation convertDatasetToJson(Dataset<Row> dataset) throws JsonProcessingException {
        List<String> jsonList = dataset.toJSON().collectAsList();
        if (!jsonList.isEmpty()) {
            log.info("Returning {} recommendations", jsonList.size());
            return JsonUtils.deserialize(jsonList.get(0), UserRecommendation.class);
        }
        log.warn("No recommendations found");
        return new UserRecommendation();
    }
}
