package com.sabbih.pepperjamservice.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

// import javax.annotation.Generated;

// @Generated("jsonschema2pojo")
@Getter
@Setter
@Data
public class Pagination {

  public Integer totalResults;
  public Integer totalPages;
  public Next next;
}
