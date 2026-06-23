package com.sabbih.meshadaaiservices.service;

import com.sabbih.meshadaaiservices.model.AiData;
import com.sabbih.meshadaaiservices.model.Product;
import com.sabbih.meshadaaiservices.utils.JsonUtils;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.Trigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
public class ProductEnrichmentService {
  private final SparkSession sparkSession;

  @Value("${datasource.url}")
  private String sqlurl;

  @Value("${datasource.username}")
  private String sqluser;

  @Value(" ${datasource.password}")
  private String sqlpwd;

  public ProductEnrichmentService(SparkSession sparkSession) {
    this.sparkSession = sparkSession;
  }

  public void processProductEnrich() {
    Dataset<Row> df =
        sparkSession
            .readStream()
            .format("jdbc")
            .option("fetchSize", "1000")
            .option("driver", "com.mysql.cj.jdbc.Driver")
            .option("url", sqlurl)
            .option("user", sqluser)
            .option("password", sqlpwd)
            .option("dbtable", "product")
            .load();

    df.writeStream()
        .trigger(Trigger.ProcessingTime("5 seconds"))
        .foreachBatch(
            (v1, v2) -> {
              WebClient webclient =
                  WebClient.builder()
                      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                      .defaultUriVariables(Collections.singletonMap("url", "http://localhost:8080"))
                      .build();

              v1.foreach(
                  t -> {
                    Product p = JsonUtils.deserialize(t.json(), Product.class);
                    webclient
                        .post()
                        .uri(
                            "http://localhost:8000/product/category?url=http://localhost:5050/sig/resize:fill:250:250:true:true/q:100/"
                                + p.getThumbnail())
                        .bodyValue(p)
                        .retrieve()
                        .onStatus(
                            HttpStatusCode::is2xxSuccessful,
                            clientResponse -> {
                              log.info("Successfully updated product category ={}");
                              AiData data = clientResponse.toEntity(AiData.class).block().getBody();
                              webclient
                                  .post()
                                  .uri("api/product/" + p.getId() + "/tags")
                                  .bodyValue(data)
                                  .retrieve()
                                  .bodyToMono(String.class);
                              return Mono.empty();
                            })
                        .toEntity(AiData.class)
                        .subscribe();
                  });
            });

    sparkSession.stop();
  }
}
