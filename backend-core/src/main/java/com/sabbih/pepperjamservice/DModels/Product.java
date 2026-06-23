package com.sabbih.pepperjamservice.DModels;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Set;
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @ToString
@Setter
public class Product {
  Long id;
  String affiliateName;
  String paymentUrl;
  String productName;
  String SKU;
  String available;
  String programIconUrl;
  String color;
  String storeName;
  String material;
  String slug;
  Set<String> tags;

  String category;
  String type;
  String thumbnail;
  Instant createdAt;
  Instant updateAt;
  Double popularity = 0.00;
  Set<String> imageUrls;

  String productDetails;

  Double price;

  Double priceOld;
  Double cashback;
  String brandName;
  String shippingPrice;
  String currency;
  String programName;
  String uuidIdentifier;
  String gender;


}
