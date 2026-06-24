package com.sabbih.meshadaaiservices.processors;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.when;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * The ALSRecommendationProcessor class is responsible for processing user interactions to generate
 * product recommendations using the ALS (Alternating Least Squares) algorithm. It reads interaction
 * data from Kafka, trains and evaluates the ALS model, and communicates with the core service to
 * update recommendations.
 *
 * <p>It contains the following methods:
 * - startALSPipe(): Scheduled method that orchestrates the entire ALS pipeline.
 * - getInteractionSchema(): Defines the schema for interaction data from Kafka.
 * - validateAndCleanData(Dataset<Row> df): Validates and cleans the interactions DataFrame.
 * - transformInteractionsToRatings(Dataset<Row> df): Transforms interactions into ratings suitable for ALS.
 * - trainAndEvaluateModel(Dataset<Row> ratings): Trains the ALS model and evaluates its performance.
 * - saveModelIfBetter(ALSModel newModel, Dataset<Row> ratings): Saves the model if it outperforms the existing one.
 * - sendRecommendationUpdate(List<String> payloads): Sends updated recommendations to the core service.
 */
@Service
@ConditionalOnProperty(name = "meshada.spark.enabled", havingValue = "true")
@Slf4j
public class ALSRecommendationProcessor implements Serializable {

    private final SparkSession sparkSession;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;

    @Value("${meshada.constants.kafka.interaction_topic}")
    private String interactionTopic;

    @Value("${datasource.url}")
    private String sqlUrl;

    @Value("${datasource.username}")
    private String sqlUser;

    @Value("${datasource.password}")
    private String sqlPwd;

//    @Value("${meshada.constants.als_path}")
    @Value("${als_path}")
    private String modelPath; // Ensure this environment variable is set

    @Value("${meshada.constants.microservice.endpoint.core_service}")
    private String coreService;

    private WebClient webClient;

    public ALSRecommendationProcessor(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(coreService)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("ALSRecommendationProcessor initialized with core service URL: {}", coreService);

        // Optionally, set Spark configurations here
//        sparkSession.conf().set("spark.executor.extraJavaOptions", "-Xss1m");
//        sparkSession.conf().set("spark.driver.extraJavaOptions", "-Xss1m");
//        sparkSession.conf().set("spark.sql.shuffle.partitions", "200"); // Adjust based on your cluster
//        sparkSession.conf().set("spark.driver.memory", "4g"); // Example value, adjust as needed
    }

    /**
     * Scheduled method to start the ALS pipeline every day at midnight.
     * Uses a cron expression to accurately schedule the job.
     */
//    @Scheduled(cron = "0 0 0 * * ?") // Every day at midnight
    @Scheduled(fixedRate = 172800000) // 48 hours in milliseconds (48 * 60 * 60 * 1000)
    public void startALSPipe() {
        try {
            log.info("Starting ALS pipeline.");

            // Read interactions data from Kafka
            Dataset<Row> df = sparkSession
                    .read()
                    .format("kafka")
                    .option("kafka.bootstrap.servers", bootstrapServer)
                    .option("subscribe", interactionTopic)
                    .option("startingOffsets", "earliest")
                    .load();
df.show();
            // Check if the DataFrame is empty by attempting to retrieve one row
            if (df.isEmpty()) {
                log.info("No data found in Kafka topic: {}", interactionTopic);
                return;
            }

            // Define schema for interactions
            StructType interactionSchema = getInteractionSchema();

            // Parse JSON data and select relevant columns
            df = df.select(col("value").cast("string"))
                    .withColumn("value", functions.from_json(col("value"), interactionSchema))
                    .select("value.*")
                    .na()
                    .fill(0L, new String[]{"id", "userId", "productId"}) // Fill nulls for Long fields
                    .na()
                    .fill("Unknown", new String[]{"interactionType"})
                    .withColumn("createdAt", functions.to_timestamp(col("createdAt")));

            log.debug("Interactions DataFrame schema:\n{}", df.schema().treeString());
            df.show();
            // Validate and clean data
            df = validateAndCleanData(df);
            df.show();
            // Transform interactions into ratings
            Dataset<Row> ratings = transformInteractionsToRatings(df).cache();
            ratings.show();
            log.debug("Ratings DataFrame schema:\n{}", ratings.schema().treeString());

            // Train and evaluate ALS model
            ALSModel model = trainAndEvaluateModel(ratings);

            // Save the model if it's better
            saveModelIfBetter(model, ratings);

            // Unpersist the ratings DataFrame to free memory
            ratings.unpersist();

            log.info("ALS pipeline completed successfully.");
        } catch (Exception e) {
            log.error("Error in ALS pipeline: ", e);
        }
    }

    /**
     * Defines the schema for interaction data from Kafka.
     *
     * @return StructType representing the interaction schema.
     */
    private StructType getInteractionSchema() {
        return new StructType()
                .add("id", DataTypes.LongType, false)
                .add("userId", DataTypes.LongType, false)
                .add("interactionType", DataTypes.StringType, false)
                .add("productId", DataTypes.LongType, false)
                .add("createdAt", DataTypes.TimestampType, false);
    }

    /**
     * Validates and cleans the interactions DataFrame.
     *
     * @param df The interactions DataFrame.
     * @return Cleaned DataFrame.
     */
    private Dataset<Row> validateAndCleanData(Dataset<Row> df) {
        // Example validation: Ensure productId and userId are positive
        Dataset<Row> cleanedDf = df.filter(col("productId").gt(0).and(col("userId").gt(0)));
        log.info("Cleaned DataFrame count: {}", cleanedDf.count());
        return cleanedDf;
    }

    /**
     * Transforms interactions DataFrame into ratings DataFrame suitable for ALS.
     *
     * @param df The interactions DataFrame.
     * @return Ratings DataFrame.
     */
    private Dataset<Row> transformInteractionsToRatings(Dataset<Row> df) {
        Dataset<Row> ratings = df.select(
                col("userId").alias("user"),
                col("productId").alias("product"),
                when(col("interactionType").equalTo("CLICK"), 5.0)
                        .when(col("interactionType").equalTo("VIEW"), 3.0)
                        .when(col("interactionType").equalTo("WISH"), 5.0)
                        .when(col("interactionType").equalTo("UNWISH"), 1.0)
                        .when(col("interactionType").equalTo("WARDROBE"), 5.0)
                        .when(col("interactionType").equalTo("UNWARDROBE"), 1.0)
                        .when(col("interactionType").equalTo("CHECKOUT"), 5.0)
                        .when(col("interactionType").equalTo("SHARED"), 2.0)
                        .otherwise(0.0).alias("rating")
        ).filter(col("rating").gt(0.0)); // Filter out non-positive ratings

        log.info("Transformed ratings count: {}", ratings.count());
        return ratings;
    }

    /**
     * Trains the ALS model and evaluates it using RMSE.
     *
     * @param ratings The ratings DataFrame.
     * @return Trained ALSModel.
     */
    private ALSModel trainAndEvaluateModel(Dataset<Row> ratings) {
        // Split data into training and test sets
        Dataset<Row>[] splits = ratings.randomSplit(new double[]{0.8, 0.2}, 42);
        Dataset<Row> training = splits[0];
        Dataset<Row> test = splits[1];

        // Initialize ALS
        ALS als = new ALS()
                .setMaxIter(5)
                .setRegParam(0.1)
                .setRank(2)
                .setUserCol("user")
                .setItemCol("product")
                .setRatingCol("rating")
                .setColdStartStrategy("drop"); // Drop NaN predictions

        log.info("Training ALS model with parameters: maxIter=10, regParam=0.1");

        // Train the model
        ALSModel model = als.fit(training);

        log.info("ALS model training completed. Evaluating model performance.");

        // Evaluate the model
        Dataset<Row> predictions = model.transform(test);
        RegressionEvaluator evaluator = new RegressionEvaluator()
                .setLabelCol("rating")
                .setPredictionCol("prediction")
                .setMetricName("rmse");

        double rmse = evaluator.evaluate(predictions);
        log.info("ALS Model RMSE on test data: {}", rmse);

        return model;
    }

    /**
     * Saves the ALS model if it's better (lower RMSE) than the existing model.
     *
     * @param newModel The newly trained ALSModel.
     * @param ratings  The ratings DataFrame used for training and evaluation.
     */
    private void saveModelIfBetter(ALSModel newModel, Dataset<Row> ratings) {
        try {
            File modelDirectory = new File(modelPath);
            if (!modelDirectory.exists()) {
                boolean dirsCreated = modelDirectory.mkdirs();
                if (dirsCreated) {
                    log.info("Created model directory at: {}", modelPath);
                } else {
                    log.error("Failed to create model directory at: {}", modelPath);
                    return;
                }
            } else {
                log.info("Model directory exists at: {}", modelPath);
            }

            // Check if an existing model exists
            ALSModel existingModel = null;
            double existingRmse = Double.MAX_VALUE;

            try {
                existingModel = ALSModel.load(modelPath);
                RegressionEvaluator evaluator = new RegressionEvaluator()
                        .setLabelCol("rating")
                        .setPredictionCol("prediction")
                        .setMetricName("rmse");
                Dataset<Row> predictions = existingModel.transform(ratings);
                existingRmse = evaluator.evaluate(predictions);
                log.info("Existing ALS Model RMSE: {}", existingRmse);
            } catch (Exception e) {
                log.warn("No existing model found or failed to load existing model: {}. Proceeding to save the new model.", e.getMessage());
            }

            // Evaluate the new model
            Dataset<Row> newPredictions = newModel.transform(ratings);
            RegressionEvaluator evaluator = new RegressionEvaluator()
                    .setLabelCol("rating")
                    .setPredictionCol("prediction")
                    .setMetricName("rmse");
            double newRmse = evaluator.evaluate(newPredictions);
            log.info("New ALS Model RMSE: {}", newRmse);

            // Compare RMSEs and decide to save the new model
            if (newRmse < existingRmse) {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String versionedModelPath = modelPath ;

                newModel.write().overwrite().save(versionedModelPath);
                log.info("Saved new better ALS model at: {}", versionedModelPath);

                // Optionally, update a symlink or latest model pointer
                // updateLatestModelPointer(versionedModelPath);
            } else {
                log.info("New model RMSE is not better than the existing model. Skipping save.");
            }
        } catch (Exception e) {
            log.error("Error in saving the ALS model: ", e);
        }
    }

    /**
     * Sends updated recommendations to the core service using WebClient.
     *
     * @param payloads The list of JSON payloads representing recommendations.
     */
    private void sendRecommendationUpdate(List<String> payloads) {
        if (payloads.isEmpty()) {
            log.info("No recommendation updates to send.");
            return;
        }

        try {
            // Build the bulk request payload
            String bulkPayload = payloads.stream()
                    .collect(Collectors.joining(",", "[", "]"));

            // Send the bulk PUT request
            webClient.put()
                    .uri("/api/product/recommendations/bulk")
                    .bodyValue(bulkPayload)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("Failed to update recommendations: " + errorBody))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(throwable -> throwable instanceof RuntimeException))
                    .subscribe(response -> log.debug("Bulk updated recommendations: {}", response),
                            error -> log.error("Error updating recommendations: {}", error.getMessage()));
        } catch (Exception e) {
            log.error("Error in sendRecommendationUpdate: ", e);
        }
    }
}
