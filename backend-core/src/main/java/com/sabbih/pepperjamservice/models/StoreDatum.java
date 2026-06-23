package com.sabbih.pepperjamservice.models;

import lombok.Data;

import javax.annotation.processing.Generated;

@Data
@Generated("jsonschema2pojo")
public class StoreDatum {

  private String id;

  private String name;

  private String description;

  private String logo;

  private String prohibited_states;

  private String mobile_tracking;

  //    private List<Category> category = null;

  private String address1;

  private String address2;

  private String city;

  private String state_code;

  private String zip_code;

  private String country_code;

  private String phone;

  private String website;

  private String contact_name;

  private String email;

  private String currency;

  private String status;

  private String join_date;

  private String cookie_duration;

  private String percentage_payout;

  private String flat_payout;

  private String deep_linking;

  private String product_feed;
}
