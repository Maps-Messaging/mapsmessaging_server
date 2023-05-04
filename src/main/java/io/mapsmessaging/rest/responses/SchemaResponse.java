package io.mapsmessaging.rest.responses;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Request;
import lombok.Getter;

public class SchemaResponse extends BaseResponse {

  @Getter
  private final List<String> data;

  public SchemaResponse(Request request, String schema) {
    super(request);
    this.data = new ArrayList<>();
    data.add(schema);
  }


  public SchemaResponse(Request request, List<String> data) {
    super(request);
    this.data = data;
  }
}
