// package com.sabbih.meshadaaiservices.listeners;
//
// import com.sabbih.meshadaaiservices.Recommender.SparkRecommenderService;
// import org.springframework.kafka.annotation.KafkaListener;
//
// public class RatingsListener {
//
//  private SparkRecommenderService sparkRecommenderService;
//
//  public RatingsListener(SparkRecommenderService sparkRecommenderService) {
//    this.sparkRecommenderService = sparkRecommenderService;
//  }
//
//  @KafkaListener(id = "groupId", topics = "${meshada.constants.kafka.rating_topic}")
//  public void consumerRatings(String ratingJson) {
//    sparkRecommenderService.startALSPipe();
//  }
// }
