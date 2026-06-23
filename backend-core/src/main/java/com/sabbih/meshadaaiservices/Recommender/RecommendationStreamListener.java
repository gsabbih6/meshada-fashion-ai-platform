// package com.sabbih.meshadaaiservices.Recommender;
//
// import lombok.extern.slf4j.Slf4j;
// import org.apache.spark.sql.SparkSession;
// import org.apache.spark.sql.streaming.StreamingQueryListener;
//
// @Slf4j
// public class RecommendationStreamListener extends StreamingQueryListener {
//
//  private SparkSession sparkSession;
//
//  public RecommendationStreamListener(SparkSession sparkSession) {
//    this.sparkSession = sparkSession;
//  }
//
//  @Override
//  public void onQueryIdle(QueryIdleEvent event) {
//    super.onQueryIdle(event);
//    //    sparkSession.stop();
//  }
//
//  @Override
//  public void onQueryStarted(QueryStartedEvent event) {
//    log.info("ALS started");
//  }
//
//  @Override
//  public void onQueryProgress(QueryProgressEvent event) {
//    // event.progress().
//    log.info("ALS in progress: " + event.progress().observedMetrics());
//  }
//
//  @Override
//  public void onQueryTerminated(QueryTerminatedEvent event) {
//    System.out.println("ALS in terminated");
//    //    sparkSession.stop();
//  }
// }
