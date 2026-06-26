package com.sabbih.pepperjamservice.DModels;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "products")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @ToString
@Setter
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  String affiliateName;

  @Column(columnDefinition = "TEXT")
  String paymentUrl;

  String productName;

  @Column(name = "sku")
  String SKU;

  String available;

  @Column(columnDefinition = "TEXT")
  String programIconUrl;

  String color;
  String storeName;
  String material;
  String slug;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
  Set<String> tags;

  String category;
  String type;

  @Column(columnDefinition = "TEXT")
  String thumbnail;

  Instant createdAt;
  Instant updateAt;

  @Builder.Default
  Double popularity = 0.00;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "product_image_urls", joinColumns = @JoinColumn(name = "product_id"))
  Set<String> imageUrls;

  @Column(columnDefinition = "TEXT")
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

  @Builder.Default
  private Boolean videoGenerated = false;
}
