// package com.sabbih.meshadaaiservices.Recommender;
//
// import java.util.List;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;
//
// @RequestMapping("recommender")
// @RestController
// public class RecommenderController {
//  @Autowired SparkRecommenderService recommenderService;
//
//  public ResponseEntity<List<Integer>> getRecommendedProductsbyUser(@RequestParam int userId) {
//    return null;
//  }
//
//  @GetMapping("/top10product")
//  public List<Integer> getTopProducts() {
//    return recommenderService.getTop10RecommendedProducts();
//  }
// }
