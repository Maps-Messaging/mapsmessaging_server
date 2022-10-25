package io.mapsmessaging.rest.data;

import lombok.Getter;
import lombok.Setter;

public class BaseResponse {

  @Getter
  @Setter
  private String data;

  public BaseResponse(String data){
    this.data = data;
  }
}
