package io.mapsmessaging.rest.responses;

import io.mapsmessaging.rest.data.ServerStatistics;
import javax.ws.rs.core.Request;
import lombok.Getter;

public class ServerStatisticsResponse extends BaseResponse {

  @Getter
  private final ServerStatistics data;

  public ServerStatisticsResponse(Request request){
    super(request);
    data = new ServerStatistics();
  }
}
