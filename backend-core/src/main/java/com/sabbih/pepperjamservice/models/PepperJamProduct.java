package com.sabbih.pepperjamservice.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PepperJamProduct {

  @JsonProperty("meta")
  private Meta meta;

  @JsonProperty("data")
  private List<PProduct> data;
}
