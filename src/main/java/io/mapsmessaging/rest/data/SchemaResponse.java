package io.mapsmessaging.rest.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class SchemaResponse {

  @Getter
  private List<SchemaData> data;

  public SchemaResponse(SchemaData schema){
    this.data = new ArrayList<>();
    data.add(schema);
  }


  public SchemaResponse(List<SchemaData> data){
    this.data = data;
  }
}
