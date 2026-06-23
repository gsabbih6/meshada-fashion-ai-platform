package com.sabbih.pepperjamservice.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CouponResponse {
  @JsonProperty("meta")
  public Meta meta;

  @JsonProperty("data")
  public List<Coupon> data;
}
