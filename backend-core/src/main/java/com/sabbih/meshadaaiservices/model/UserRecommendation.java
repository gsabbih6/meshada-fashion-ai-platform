package com.sabbih.meshadaaiservices.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class UserRecommendation {
  private Integer user;
  private List<Recommendation> recommendations;
}
