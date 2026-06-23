package com.sabbih.pepperjamservice.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

// @Generated("jsonschema2pojo")
@Getter
@Setter
@Data
public class Transaction {

  public String transactionId;
  public String orderId;
  public String creativeType;
  public String commission;
  public String saleAmount;
  public String type;
  public String date;
  public String status;
  public String newToFile;
  public String publisherReferralUrl;
  public String subType;
  public String sid;
  public String programName;
  public String programId;
}
