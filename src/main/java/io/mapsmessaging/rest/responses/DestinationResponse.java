package io.mapsmessaging.rest.responses;

import io.mapsmessaging.rest.data.Destination;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Request;
import lombok.Getter;

public class DestinationResponse extends BaseResponse {

  @Getter
  private final List<Destination> data;

  public DestinationResponse(Request request, Destination data ){
    super(request);
    this.data = new ArrayList<>();
    this.data.add(data);
  }


  public DestinationResponse(Request request, List<Destination> data ){
    super(request);
    this.data = data;
  }
}
