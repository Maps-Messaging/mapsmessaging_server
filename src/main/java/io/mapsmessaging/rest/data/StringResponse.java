package io.mapsmessaging.rest.data;

import lombok.Getter;

public class StringResponse {

  @Getter
  private String data;

  public StringResponse(String data){
    this.data = data;
  }
}
