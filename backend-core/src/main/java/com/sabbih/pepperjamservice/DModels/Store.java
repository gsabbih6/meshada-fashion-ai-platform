package com.sabbih.pepperjamservice.DModels;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

//@Table
//@Data
//@Table(name = "Stores")
@NoArgsConstructor
@AllArgsConstructor
//@Entity
//@Builder
//@EqualsAndHashCode
@Getter
@Setter
public class Store {

    //    @PrimaryKey
//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    long id;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;

    private String name;
    private String description;
    private String logo;
    private String prohibitedStates;
    private String mobileTracking;
    //    private List<Category> category = null;
    private String address1;
    private String address2;
    private String city;
    private String stateCode;
    private String zipCode;
    private String countryCode;
    private String phone;
    private String website;
    private String contactName;
    private String email;
    private String currency;
    private String status;
    private String joinDate;
    private String cookieDuration;
    private String percentagePayout;
    private String flatPayout;
    private String deepLinking;
    private String productFeed;
    private String code;
    private String networkName;
    @JsonIgnore
  @OneToMany(mappedBy = "store", fetch = FetchType.EAGER,cascade=CascadeType.ALL)
    private Set<Product> products;

    public Store(String name) {
        this.name = name;
    }
}
