package com.sabbih.pepperjamservice.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaymentDetails {
  @JsonProperty("payment_id")
  private String paymentId;

  @JsonProperty("transaction_id")
  private String transactionId;

  @JsonProperty("order_id")
  private String orderId;

  @JsonProperty("item_id")
  private String itemId;

  @JsonProperty("sid")
  private String
      sid; // in my case sid will be each users id so i can find out who made the purchase

  @JsonProperty("program_id")
  private String programId;

  @JsonProperty("program_name")
  private String programName;

  @JsonProperty("creative_type")
  private String creativeType;

  @JsonProperty("commission")
  private String commission;

  @JsonProperty("sale_amount")
  private String saleAmount;

  @JsonProperty("transaction_type")
  private String transactionType;

  @JsonProperty("transaction_date")
  private String transactionDate;

  @JsonProperty("payment_date")
  private String paymentDate;
}
