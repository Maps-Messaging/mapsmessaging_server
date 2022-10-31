package io.mapsmessaging.rest.responses;

import io.mapsmessaging.rest.data.SchemaData;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Request;
import lombok.Getter;

public class SchemaResponse extends BaseResponse {

  @Getter
  private final List<SchemaData> data;

  public SchemaResponse(Request request, SchemaData schema){
    super(request);
    this.data = new ArrayList<>();
    data.add(schema);
  }


  public SchemaResponse(Request request, List<SchemaData> data){
    super(request);
    this.data = data;
  }
}
