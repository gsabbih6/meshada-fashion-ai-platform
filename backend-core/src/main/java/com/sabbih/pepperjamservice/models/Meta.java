package com.sabbih.pepperjamservice.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

// import javax.annotation.Generated;
// import javax.validation.Valid;
@Getter
@Setter
@Data
public class Meta {

  //    @Valid
  public Status status;
  //    @Valid
  public Pagination pagination;
  //    @Valid
  public Requests requests;
}
