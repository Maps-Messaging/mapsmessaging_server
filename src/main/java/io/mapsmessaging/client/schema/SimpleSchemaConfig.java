package io.mapsmessaging.client.schema;

import org.json.JSONObject;

class SimpleSchemaConfig extends SchemaConfig {

  public SimpleSchemaConfig(String format) {
    super(format);
  }

  @Override
  protected JSONObject packData() {
    JSONObject data = new JSONObject();
    packData(data);
    return data;
  }
}