package com.sabbih.meshadaaiservices.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tags {
  private String gender;
  private String category;

  @JsonProperty("sub_categories")
  private List<String> subCategories=new ArrayList<>();

  private String color;
  private String material;
}
