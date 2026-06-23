package com.sabbih.pepperjamservice.DModels;

import jakarta.persistence.*;
import lombok.*;

// @Entity
// @Data
@Builder
@Table
@NoArgsConstructor
@AllArgsConstructor
//@Entity
@Getter
@Setter
public class Payment { // sort of same as cashback
  private String paymentId;
  private String transactionId;
  private String orderId;
  private String itemId;
  private String
      sid; // in my case sid will be each users id so i can find out who made the purchase
  private String programId;
  private String programName;
  private String creativeType;
  private String commission;
  private String saleAmount;
  private String transactionType;
  private String transactionDate;
  private String paymentDate;
  private String status;

//  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // let this be the same as payment id from the program but UUID
}
