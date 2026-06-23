package com.sabbih.meshadaaiservices.processors;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

/**
 * The InteractionsProcessor class is responsible for processing product interactions. It uses
 * SparkSession and a WebClient to load data from Kafka and calculate the popularity score for each
 * item in the dataset.
 *
 * <p>It contains the following methods:
 * - processProductInteractions(): Reads data from Kafka, converts it into a DataFrame, and calls the calculatePopularity method.
 * - calculatePopularity(Dataset<Row> df): Calculates the popularity score for each item in the dataset by assigning weights and scores based on the type and age of the interaction. It then aggregates the scores by item and sends the results to the core service using a WebClient.
 */
@Service
@Slf4j
public class InteractionsProcessor implements Serializable {

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

    @Value("${meshada.constants.microservice.endpoint.core_service}")
    private String coreService;

    private WebClient webClient;

    public InteractionsProcessor(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(coreService)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("WebClient initialized with base URL: {}", coreService);
    }

    /**
     * Scheduled method to process product interactions every 24 hours.
     */
//    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000) // every 2x4 hours in milliseconds
    @Scheduled(cron = "0 0 0 */3 * ?")
    public void processProductInteractions() {
        try {
            log.info("Starting processProductInteractions job.");
            Dataset<Row> df = sparkSession
                    .read()
                    .format("kafka")
                    .option("kafka.bootstrap.servers", bootstrapServer)
                    .option("subscribe", interactionTopic)
                    .option("startingOffsets", "earliest")
                    .load();

            // Check if the DataFrame is empty by attempting to retrieve one row
            if (df.isEmpty()) {
                log.info("No data found in Kafka topic: {}", interactionTopic);
                return;
            }

            StructType schema = getSchema();

            df = df.select(col("value").cast("string"))
                    .withColumn("value", from_json(col("value"), schema))
                    .select("value.*")
                    .na()
                    .fill(0L, new String[]{"id", "userId", "productId"}) // Fill nulls for Long fields
                    .na()
                    .fill("Unknown", new String[]{"interactionType"})
                    .withColumn("createdAt", to_timestamp(col("createdAt")));

            calculatePopularity(df);
            log.info("Completed processProductInteractions job.");
        } catch (Exception e) {
            log.error("Error in processProductInteractions: ", e);
        }
    }

    /**
     * Defines the schema for the incoming Kafka data.
     */
    private StructType getSchema() {
        return new StructType()
                .add("id", DataTypes.LongType, false)
                .add("userId", DataTypes.LongType, false)
                .add("interactionType", DataTypes.StringType, false)
                .add("productId", DataTypes.LongType, false)
                .add("createdAt", DataTypes.TimestampType, false);
    }

    /**
     * Calculates the popularity score for each item in the dataset.
     *
     * @param df The dataset containing the interaction data.
     */
    public void calculatePopularity(Dataset<Row> df) {
        try {
            log.info("Starting calculatePopularity.");

            // Calculate interaction age in hours
            df = df.withColumn("interaction_age",
                    (unix_timestamp(current_timestamp())
                            .minus(unix_timestamp(col("createdAt"))))
                            .divide(3600));

            // Assign weights based on interaction age
            df = df.withColumn("weight",
                    when(col("interaction_age").lt(1), 1.0)
                            .when(col("interaction_age").lt(24), 0.8)
                            .when(col("interaction_age").lt(7 * 24), 0.5)
                            .when(col("interaction_age").lt(30 * 24), 0.2)
                            .otherwise(0.1));

            // Assign scores based on interaction type
            df = df.withColumn("score",
                    col("weight").multiply(
                            when(col("interactionType").equalTo("CLICK"), 1.0)
                                    .when(col("interactionType").equalTo("VIEW"), 0.8)
                                    .when(col("interactionType").equalTo("WISH"), 0.7)
                                    .when(col("interactionType").equalTo("UNWISH"), 0.2)
                                    .when(col("interactionType").equalTo("WARDROBE"), 0.7)
                                    .when(col("interactionType").equalTo("UNWARDROBE"), 0.2)
                                    .when(col("interactionType").equalTo("SHARED"), 0.9)
                                    .otherwise(0.0)
                    ));

            if (log.isDebugEnabled()) {
                log.debug("DataFrame after assigning scores:");
                df.show(false);
            }

            // Aggregate scores by productId
            Dataset<Row> popularityScores = df.groupBy("productId")
                    .agg(functions.sum("score").alias("popularity_score"));

            // Normalize the popularity scores between 0 and 1
            Row minRow = popularityScores.agg(functions.min("popularity_score")).first();
            Row maxRow = popularityScores.agg(functions.max("popularity_score")).first();
            double min = minRow.getDouble(0);
            double max = maxRow.getDouble(0);

            if (max == min) {
                popularityScores = popularityScores.withColumn("popularity_score_normal", functions.lit(1.0));
                log.warn("All popularity scores are equal. Normalized scores set to 1.0.");
            } else {
                popularityScores = popularityScores.withColumn("popularity_score_normal",
                        (col("popularity_score").minus(min)).divide(max - min));
            }

            if (log.isDebugEnabled()) {
                log.debug("DataFrame after normalization:");
                popularityScores.show(false);
            }

            // Send the results to the core service
            sendPopularityScores(popularityScores);
            log.info("Completed calculatePopularity.");
        } catch (Exception e) {
            log.error("Error in calculatePopularity: ", e);
        }
    }

    /**
     * Sends the calculated popularity scores to the core service using WebClient.
     *
     * @param popularityScores The dataset containing the popularity scores.
     */
    private void sendPopularityScores(Dataset<Row> popularityScores) {
        log.info("Sending popularity scores to core service.");
        try {
            // Collect all records to a list for batching
            List<Row> scoreList = popularityScores.collectAsList();
            if (scoreList.isEmpty()) {
                log.info("No popularity scores to send.");
                return;
            }

            // Build the request payload as a JSON array
            String bulkPayload = scoreList.stream()
                    .map(row -> String.format("{\"productId\": %d, \"popularity\": %.4f}",
                            row.getLong(row.fieldIndex("productId")),
                            row.getDouble(row.fieldIndex("popularity_score_normal"))))
                    .collect(Collectors.joining(",", "[", "]"));

            // Send the bulk PUT request
            webClient.put()
                    .uri("/api/product/popularity/bulk")
                    .bodyValue(bulkPayload)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("Failed to update popularity: " + errorBody))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(throwable -> throwable instanceof RuntimeException))
                    .subscribe(response -> log.debug("Bulk updated popularity scores: {}", response),
                            error -> log.error("Error updating popularity scores: {}", error.getMessage()));
        } catch (Exception e) {
            log.error("Error in sendPopularityScores: ", e);
        }
    }
}
