// package com.sabbih.meshadaaiservices.Recommender;
//
// import static org.apache.spark.sql.functions.col;
// import static org.apache.spark.sql.functions.explode;
//
// import java.io.File;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.TimeoutException;
// import lombok.extern.slf4j.Slf4j;
// import org.apache.spark.ml.evaluation.RegressionEvaluator;
// import org.apache.spark.ml.recommendation.ALS;
// import org.apache.spark.ml.recommendation.ALSModel;
// import org.apache.spark.sql.*;
// import org.apache.spark.sql.streaming.StreamingQueryException;
// import org.apache.spark.sql.streaming.Trigger;
// import org.apache.spark.sql.types.DataTypes;
// import org.apache.spark.sql.types.StructType;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import scala.collection.mutable.ArraySeq;
//
// @Service
// @Slf4j
// public class SparkRecommenderService {
//  //    @Autowired
//  private SparkSession sparkSession;
//
//  @Value(value = "${spring.kafka.bootstrap-servers}")
//  private String bootstrapserver;
//
//  @Value(value = "${meshada.constants.kafka.rating_topic}")
//  private String ratingTopic;
//
//  @Value("${datasource.url}")
//  private String sqlurl;
//
//  @Value("${datasource.username}")
//  private String sqluser;
//
//  @Value("${datasource.password}")
//  private String sqlpwd;
//
//  public SparkRecommenderService(SparkSession sparkSession) {
//    this.sparkSession = sparkSession;
//    sparkSession.streams().addListener(new RecommendationStreamListener(sparkSession));
//  }
//
//  public void startALSPipe() {
//    StructType ratingStruct =
//        new StructType()
//            .add("user", DataTypes.IntegerType)
//            .add("product", DataTypes.IntegerType)
//            .add("rating", DataTypes.DoubleType);
//    Dataset<Row> df =
//        sparkSession
//            .readStream()
//            .format("kafka")
//            .option("kafka.bootstrap.servers", bootstrapserver)
//            .option("subscribe", ratingTopic)
//            .load();
//
//    // Basic ALS model
//    ALS als =
//        new ALS()
//            .setMaxIter(5)
//            .setRegParam(0.01)
//            .setUserCol("user")
//            .setRatingCol("rating")
//            .setItemCol("product");
//
//    try {
//      df.select(col("value").cast("string"))
//          .withColumn("value", functions.from_json(col("value"), ratingStruct))
//          .select("value.*")
//          .writeStream()
//          .trigger(Trigger.ProcessingTime("10 seconds"))
//          .foreachBatch(
//              (batchDF, batchId) -> {
//                if (batchDF.count() > 10) {
//                  // Split the batch into train and test sets
//                  batchDF = batchDF.na().drop();
//                  Dataset<Row>[] splits = batchDF.randomSplit(new double[] {0.8, 0.2}, 42);
//                  Dataset<Row> train = splits[0];
//                  Dataset<Row> test = splits[1];
//
//                  //                                    batchDF.show();
//
//                  //                            Start training
//                  ALSModel model = als.fit(train);
//
//                  // Evaluate the ALS model on the test set
//                  Dataset<Row> predictions = model.transform(test);
//                  RegressionEvaluator evaluator =
//                      new RegressionEvaluator()
//                          .setLabelCol("rating")
//                          .setPredictionCol("prediction")
//                          .setMetricName("rmse");
//                  double rmse = evaluator.evaluate(predictions);
//
//                  log.info(evaluator.toString());
//                  log.info("Recommended Users");
//                  //                            model.recommendForAllItems(10).show();
//
//                  //                           Save the model if it has the best R.M.S.E so far
//                  String checkpointPath = "src/main/resources/static/ALS/checkpoint";
//                  String modelPath = "src/main/resources/static/ALS/model";
//
//                  File checkPointFilePath = new File(checkpointPath);
//                  File modelFilePath = new File(checkpointPath);
//
//                  if (!checkPointFilePath.exists()) checkPointFilePath.mkdirs();
//                  if (!modelFilePath.exists()) modelFilePath.mkdirs();
//
//                  if (modelFilePath.listFiles().length > 0) { // is there any model saved ?
//                    ALSModel bestModel = ALSModel.load(modelPath);
//                    if (rmse < evaluator.evaluate(bestModel.transform(test))) {
//                      model
//                          .write()
//                          .overwrite()
//                          .option("checkpointLocation", checkpointPath)
//                          .overwrite()
//                          .save(modelPath);
//                    }
//                  } else {
//                    model
//                        .write()
//                        .overwrite()
//                        .option("checkpointLocation", checkpointPath)
//                        .overwrite()
//                        .save(modelPath);
//                  }
//
//                  // update pre-calculated predictions
//                  startPrecalculation(model);
//                }
//              })
//          .start()
//          .awaitTermination();
//    } catch (TimeoutException | StreamingQueryException e) {
//      throw new RuntimeException(e);
//    }
//  }
//
//  private void startPrecalculation(ALSModel model) throws TimeoutException {
//    Dataset<Row> topforusersDF = model.recommendForAllUsers(100);
//    topforusersDF
//        .flatMap(
//            (Row row) -> {
//              int user = row.getInt(0);
//              ArraySeq<Row> recommendations = (ArraySeq<Row>) row.get(1);
//              List<Row> rows = new ArrayList<>();
//              recommendations.foreach(
//                  rec -> {
//                    int itemId = rec.getInt(0);
//                    float rating = rec.getFloat(1);
//                    Row newRow = RowFactory.create(user, itemId, rating);
//                    rows.add(newRow);
//                    return null;
//                  });
//              return rows.iterator();
//            },
//            Encoders.row(
//                new StructType()
//                    .add("user_id", DataTypes.IntegerType, false)
//                    .add("product_id", DataTypes.IntegerType, false)
//                    .add("rating", DataTypes.FloatType, false)))
//        .write()
//        .mode(SaveMode.Overwrite)
//        .format("jdbc")
//        .option("truncate", "true")
//        .option("driver", "com.mysql.cj.jdbc.Driver")
//        .option("url", sqlurl)
//        .option("user", sqluser)
//        .option("password", sqlpwd)
//        .option("dbtable", "recommended_product")
//        .save();
//  }
//
//  public List<Integer> getTop10RecommendedProducts() {
//    String modelPath = "src/main/resources/static/ALS/model";
//    ALSModel model = ALSModel.load(modelPath);
//    final List<Integer> list = new ArrayList<>();
//    Dataset<Row> users_recommendation = model.recommendForAllUsers(10);
//    users_recommendation
//        .select(col("recommendations"), explode(col("recommendations")).alias("recommendation"))
//        .select("recommendation.*")
//        .sort(col("rating").desc())
//        .select(col("product"))
//        .takeAsList(10)
//        .forEach(
//            row -> {
//              list.add(row.getInt(0));
//            });
//    return list;
//  }
//
//  public Dataset<Row> getTop10PublicWardrope(int userid) {
//    String modelPath = "src/main/resources/static/ALS/model";
//    ALSModel model = ALSModel.load(modelPath);
//    Dataset<Row> users_recommendation = model.recommendForAllUsers(100);
//    return users_recommendation;
//  }
// }
