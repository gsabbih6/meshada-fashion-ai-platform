package com.sabbih.pepperjamservice.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PProduct {

  @JsonProperty("program_id")
  private String programId;

  @JsonProperty("program_name")
  private String programName;

  @JsonProperty("currency")
  private String currency;

  @JsonProperty("age_range")
  private String ageRange;

  @JsonProperty("artist")
  private String artist;

  @JsonProperty("aspect_ratio")
  private String aspectRatio;

  @JsonProperty("author")
  private String author;

  @JsonProperty("battery_life")
  private String batteryLife;

  @JsonProperty("binding")
  private String binding;

  @JsonProperty("buy_url")
  private String buyUrl;

  @JsonProperty("color")
  private String color;

  @JsonProperty("color_output")
  private String colorOutput;

  @JsonProperty("condition")
  private String condition;

  @JsonProperty("description_long")
  private String descriptionLong;

  @JsonProperty("director")
  private String director;

  @JsonProperty("display_type")
  private String displayType;

  @JsonProperty("edition")
  private String edition;

  @JsonProperty("expiration_date")
  private String expirationDate;

  @JsonProperty("features")
  private String features;

  @JsonProperty("focus_type")
  private String focusType;

  @JsonProperty("format")
  private String format;

  @JsonProperty("functions")
  private String functions;

  @JsonProperty("genre")
  private String genre;

  @JsonProperty("heel_height")
  private String heelHeight;

  @JsonProperty("height")
  private String height;

  @JsonProperty("image_thumb_url")
  private String imageThumbUrl;

  @JsonProperty("image_url")
  private String imageUrl;

  @JsonProperty("installation")
  private String installation;

  @JsonProperty("isbn")
  private String isbn;

  @JsonProperty("length")
  private String length;

  @JsonProperty("load_type")
  private String loadType;

  @JsonProperty("location")
  private String location;

  @JsonProperty("made_in")
  private String madeIn;

  @JsonProperty("manufacturer")
  private String manufacturer;

  @JsonProperty("material")
  private String material;

  @JsonProperty("megapixels")
  private String megapixels;

  @JsonProperty("memory_type")
  private String memoryType;

  @JsonProperty("memory_capacity")
  private String memoryCapacity;

  @JsonProperty("memory_card_slot")
  private String memoryCardSlot;

  @JsonProperty("model_number")
  private String modelNumber;

  @JsonProperty("mpn")
  private String mpn;

  @JsonProperty("name")
  private String name;

  @JsonProperty("occasion")
  private String occasion;

  @JsonProperty("operating_system")
  private String operatingSystem;

  @JsonProperty("optical_drive")
  private String opticalDrive;

  @JsonProperty("price_retail")
  private String priceRetail;

  @JsonProperty("pages")
  private String pages;

  @JsonProperty("payment_accepted")
  private String paymentAccepted;

  @JsonProperty("payment_notes")
  private String paymentNotes;

  @JsonProperty("platform")
  private String platform;

  @JsonProperty("price_sale")
  private String priceSale;

  @JsonProperty("processor")
  private String processor;

  @JsonProperty("publisher")
  private String publisher;

  @JsonProperty("quantity_in_stock")
  private String quantityInStock;

  @JsonProperty("rating")
  private String rating;

  @JsonProperty("recommended_usage")
  private String recommendedUsage;

  @JsonProperty("resolution")
  private String resolution;

  @JsonProperty("shoe_size")
  private String shoeSize;

  @JsonProperty("screen_size")
  private String screenSize;

  @JsonProperty("shipping_method")
  private String shippingMethod;

  @JsonProperty("price_shipping")
  private String priceShipping;

  @JsonProperty("shoe_width")
  private String shoeWidth;

  @JsonProperty("size")
  private String size;

  @JsonProperty("sku")
  private String sku;

  @JsonProperty("staring")
  private String staring;

  @JsonProperty("style")
  private String style;

  @JsonProperty("tracks")
  private String tracks;

  @JsonProperty("upc")
  private String upc;

  @JsonProperty("weight")
  private String weight;

  @JsonProperty("width")
  private String width;

  @JsonProperty("wireless_interface")
  private String wirelessInterface;

  @JsonProperty("year")
  private String year;

  @JsonProperty("zoom")
  private String zoom;

  @JsonProperty("category_network")
  private String categoryNetwork;

  @JsonProperty("category_program")
  private String categoryProgram;

  @JsonProperty("description_short")
  private String descriptionShort;

  @JsonProperty("discontinued")
  private String discontinued;

  @JsonProperty("in_stock")
  private String inStock;

  @JsonProperty("tech_spec_url")
  private String techSpecUrl;

  @JsonProperty("keywords")
  private String keywords;

  @JsonProperty("price")
  private String price;
}
