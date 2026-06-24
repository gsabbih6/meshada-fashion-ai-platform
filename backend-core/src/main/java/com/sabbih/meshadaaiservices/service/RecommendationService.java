package com.sabbih.meshadaaiservices.service;

import com.sabbih.meshadaaiservices.utils.ModelLoader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "meshada.spark.enabled", havingValue = "true")
@Slf4j
public class RecommendationService implements Serializable {

//    @Value("${meshada.constants.als_path}")
    @Value("${als_path}") // Reads from environment variable or defaults to empty string

    private String modelPath;  // Standardized method to retrieve the path

    private static final StructType USER_SCHEMA = new StructType(new StructField[]{
            new StructField("user", DataTypes.IntegerType, false, Metadata.empty())
    });

    private final SparkSession sparkSession;
    private ALSModel model;

    public RecommendationService(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    @PostConstruct
    private void init() {
        if (modelPath == null || modelPath.isEmpty()) {
            throw new IllegalArgumentException("Model path is not set. Ensure 'meshada.constants.als_path' is configured.");
        }
        try {
            this.model = ModelLoader.loadLatestModel(modelPath);
            log.info("ALS Model successfully loaded from: {}", modelPath);
        } catch (Exception e) {
            log.error("Failed to load ALS model from path: {}. Error: {}", modelPath, e.getMessage());
            // Implement fallback logic here if needed
            this.model = null;
        }
    }

    public Dataset<Row> recommendProductsForUser(int userId, int numRecommendations) {
        if (model == null) {
            log.warn("ALS Model is not loaded. Returning empty dataset.");
            return sparkSession.emptyDataFrame();
        }
        Dataset<Row> userDataset = createUserDataset(userId);
        return model.recommendForUserSubset(userDataset, numRecommendations);
    }

    public Dataset<Row> recommendProductsForAll(int numRecommendations) {
        if (model == null) {
            log.warn("ALS Model is not loaded. Returning empty dataset.");
            return sparkSession.emptyDataFrame();
        }
        return model.recommendForAllUsers(numRecommendations);
    }

    private Dataset<Row> createUserDataset(int userId) {
        List<Row> userData = Collections.singletonList(RowFactory.create(userId));
        return sparkSession.createDataFrame(userData, USER_SCHEMA);
    }
}
