package com.sabbih.pepperjamservice.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.processing.Generated;
import java.util.List;

@Generated("jsonschema2pojo")
public class PepperJamStore {

  @JsonProperty("meta")
  private Meta meta;

  @JsonProperty("data")
  private List<StoreDatum> data = null;

  public Meta getMeta() {
    return meta;
  }

  public void setMeta(Meta meta) {
    this.meta = meta;
  }

  public List<StoreDatum> getData() {
    return data;
  }

  public void setData(List<StoreDatum> data) {
    this.data = data;
  }
}
