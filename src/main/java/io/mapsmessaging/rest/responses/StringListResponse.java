package io.mapsmessaging.rest.responses;

import java.util.List;
import javax.ws.rs.core.Request;
import lombok.Getter;

public class StringListResponse extends BaseResponse {

  @Getter
  private final List<String> data;

  public StringListResponse(Request request, List<String> data){
    super(request);
    this.data = data;
  }
}
