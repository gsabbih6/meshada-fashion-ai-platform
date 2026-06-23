package com.sabbih.pepperjamservice.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Coupon {
  public String id;

  @JsonProperty("name")
  public String name;

  @JsonProperty("code")
  public String code;

  @JsonProperty("description")
  public String description;

  @JsonProperty("start_date")
  public String startDate;

  @JsonProperty("end_date")
  public String endDate;

  @JsonProperty("status")
  public String status;

  @JsonProperty("exclusive")
  public String exclusive;

  @JsonProperty("coupon")
  public String coupon;

  @JsonProperty("program_id")
  public String programId;

  @JsonProperty("program_name")
  public String programName;

  @JsonProperty("category_name")
  public String categoryName;

  @JsonProperty("category_names")
  public List<String> categoryNames;
}
