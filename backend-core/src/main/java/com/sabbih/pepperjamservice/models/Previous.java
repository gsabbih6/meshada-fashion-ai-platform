package com.sabbih.pepperjamservice.models;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.processing.Generated;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"rel", "href", "description"})
@Generated("jsonschema2pojo")
public class Previous {

  @JsonProperty("rel")
  private String rel;

  @JsonProperty("href")
  private String href;

  @JsonProperty("description")
  private String description;

  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("rel")
  public String getRel() {
    return rel;
  }

  @JsonProperty("rel")
  public void setRel(String rel) {
    this.rel = rel;
  }

  @JsonProperty("href")
  public String getHref() {
    return href;
  }

  @JsonProperty("href")
  public void setHref(String href) {
    this.href = href;
  }

  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  @Override
  public String toString() {
    return "Previous{"
        + "rel='"
        + rel
        + '\''
        + ", href='"
        + href
        + '\''
        + ", description='"
        + description
        + '\''
        + ", additionalProperties="
        + additionalProperties
        + '}';
  }
}
