package com.sabbih.pepperjamservice.utils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Request {
  String url;
  String apiKey;
  String format;
  String programid;
  String startDate;
  String endDate;

  public String getProductRequest() {
    if (!getUrl().isEmpty()
        && !getFormat().isEmpty()
        && !getProgramid().isEmpty()
        && !getApiKey().isEmpty()) {
      return url + "apiKey=" + apiKey + "&" + "format=" + format + "&" + "programIds=" + programid;
    }
    return "";
  }

  public String getPaymentRequest() {
    if (!getUrl().isEmpty()
        && !getFormat().isEmpty()
        && !getStartDate().isEmpty()
        && !getEndDate().isEmpty()) {
      return url
          + "&apiKey="
          + apiKey
          + "&format="
          + format
          + "&startDate="
          + startDate
          + "&endDate="
          + endDate;
    }
    return "";
  }
}
