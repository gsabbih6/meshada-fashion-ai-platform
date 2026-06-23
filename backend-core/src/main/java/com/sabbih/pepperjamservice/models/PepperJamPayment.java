package com.sabbih.pepperjamservice.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"meta", "data"})
public class PepperJamPayment {

  @JsonProperty("meta")
  private Meta meta;

  @JsonProperty("data")
  private List<PaymentDetails> data = null;

  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<String, Object>();
}
