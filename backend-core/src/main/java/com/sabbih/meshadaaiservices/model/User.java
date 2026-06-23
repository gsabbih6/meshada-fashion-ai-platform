package com.sabbih.meshadaaiservices.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
  private long id;
  private String email;
  private String stripeId = "";
  private boolean stripeChargesEnabled;
  private String firstname;
  private String lastname;
  private String phonenumber;
  private String addresslineone;
  private String addressline2;
  private String zipcode;
  private String currency = "USD";
  private String state;
  private String name;
  private String localey;
  private String password;
  private String photourl;
  private String authtoken;
  private boolean verifiedEmail;
  private Double availableCashback;
  private Double paidCashback;
}
