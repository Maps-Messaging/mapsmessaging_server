package io.mapsmessaging.rest.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class StringListResponse {

  @Getter
  private List<String> data;

  public StringListResponse(String str){
    this.data = new ArrayList<>();
    data.add(str);
  }


  public StringListResponse(List<String> data){
    this.data = data;
  }
}
