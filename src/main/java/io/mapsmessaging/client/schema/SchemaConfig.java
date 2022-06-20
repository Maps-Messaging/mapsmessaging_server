package io.mapsmessaging.client.schema;

import lombok.Getter;
import org.json.JSONObject;

public abstract class SchemaConfig {

  @Getter
  private String format;


  public SchemaConfig(String format){
    this.format = format;
  }

  public String pack(){
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("schema", packData());
    return jsonObject.toString(2);
  }

  protected void packData(JSONObject jsonObject){
    jsonObject.put("format", format);
  }

  protected abstract JSONObject packData();
}
