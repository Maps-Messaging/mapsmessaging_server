package io.mapsmessaging.rest.responses;

import io.mapsmessaging.rest.data.DestinationStatus;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Request;
import lombok.Getter;

public class DestinationStatusResponse extends BaseResponse {

  @Getter
  private final List<DestinationStatus> data;

  public DestinationStatusResponse(Request request, DestinationStatus data ){
    super(request);
    this.data = new ArrayList<>();
    this.data.add(data);
  }


  public DestinationStatusResponse(Request request, List<DestinationStatus> data ){
    super(request);
    this.data = data;
  }
}
