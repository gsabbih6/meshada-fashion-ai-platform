package com.sabbih.pepperjamservice.DModels;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
//@Entity
@Getter
@Setter
public class Deal {
  private String name;
  private String code;
  private String description;
  private Instant startDate;
  private Instant endDate;
  private String status;
  private String exclusive;
  private String coupon;
  private String programId;
  private String programName;
  private String categoryName;

//  @ManyToOne(fetch = FetchType.LAZY)
//  @JoinColumn(name = "store_id")
  private Store store;

  //  @ElementCollection(fetch = FetchType.EAGER)   @CollectionTable(
  //          name = "deal_category_names",
  //          joinColumns = @JoinColumn(name = "deal_id")
  //  )
  //  private List<String> categoryNames;

//  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
}
