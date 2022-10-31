package io.mapsmessaging.rest.responses;

import javax.ws.rs.core.Request;
import lombok.Getter;

public class StringResponse extends BaseResponse {

  @Getter
  private final String data;

  public StringResponse(Request request, String data){
    super(request);
    this.data = data;
  }
}
