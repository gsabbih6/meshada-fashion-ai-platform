// package com.sabbih.meshadaaiservices.listeners;
//
// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.sabbih.meshadaaiservices.model.Rating;
// import com.sabbih.meshadaaiservices.model.Product;
// import com.sabbih.meshadaaiservices.model.User;
// import com.sabbih.meshadaaiservices.utils.JsonUtils;
// import java.io.Serializable;
// import java.util.ArrayList;
// import java.util.List;
// import lombok.extern.slf4j.Slf4j;
// import org.apache.spark.sql.Dataset;
// import org.apache.spark.sql.Row;
// import org.apache.spark.sql.SparkSession;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Component;
//
// @Slf4j
// @Component
// public class UserListener implements Serializable {
//  private transient KafkaTemplate<String, String> kafkaTemplate;
//  private SparkSession sparkSession;
//
//  @Value("${datasource.url}")
//  private String sqlurl;
//
//  @Value("${datasource.username}")
//  private String sqluser;
//
//  @Value(" ${datasource.password}")
//  private String sqlpwd;
//
//  public UserListener(SparkSession sparkSession, KafkaTemplate KafkaTemplate) {
//    this.sparkSession = sparkSession;
//    this.kafkaTemplate = KafkaTemplate;
//  }
//
//  @KafkaListener(id = "groupId", topics = "${meshada.constants.kafka.signup_topic}")
//  public void consumerUser(String userJson) {
//    //        sparkRecommenderService.startALSPipe();
//    // for new user build initial ratings
//    log.info("New user: " + userJson);
//    // parse user
//    try {
//      User user = JsonUtils.deserialize(userJson, User.class);
//      //            log.info("Neew user: "+userJson);
//      getInitialUserProductRatings(user.getId());
//    } catch (JsonProcessingException e) {
//      throw new RuntimeException(e);
//    }
//  }
//
//  public List<Rating> getInitialUserProductRatings(long user) {
//    List<Rating> ratings = new ArrayList<>();
//
//    //    productService.findAll()
//
//    Dataset<Row> df =
//        sparkSession
//            .read()
//            .format("jdbc")
//            .option("fetchSize", "1000")
//            .option("driver", "com.mysql.cj.jdbc.Driver")
//            .option("url", sqlurl)
//            .option("user", sqluser)
//            .option("password", sqlpwd)
//            .option("dbtable", "product")
//            .load();
//
//    List<Row> rowList = df.collectAsList();
//
//    rowList
//        .stream()
//        .forEach(
//            row -> {
//              Rating r = new Rating();
//              Product p = null;
//              try {
//                p = JsonUtils.deserialize(row.json(), Product.class);
//              } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//              }
//              r.setProduct((int) p.getId());
//              r.setUser((int) user);
//              r.setRating(0.00);
//              //                    log.info(String.valueOf(r));
//              String json = null;
//              try {
//                json = JsonUtils.serialize(r);
//              } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//              }
//              log.info(json);
//              kafkaTemplate.send("RATING", json);
//            });
//
//    return ratings;
//  }
// }
