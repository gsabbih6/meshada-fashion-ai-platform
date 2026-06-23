package com.sabbih.pepperjamservice.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// import javax.annotation.Generated;
// import javax.validation.Valid;

// @Generated("jsonschema2pojo")
@Getter
@Setter
@Data
public class TransactionDetails {

  //    @Valid
  public Meta meta;
  //    @Valid
  public List<Transaction> data;
}
