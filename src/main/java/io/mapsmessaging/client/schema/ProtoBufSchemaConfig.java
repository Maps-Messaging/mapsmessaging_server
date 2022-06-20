package io.mapsmessaging.client.schema;

import java.util.Base64;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class ProtoBufSchemaConfig extends SchemaConfig {

  @Getter
  @Setter
  private byte[] descriptor;

  @Getter
  @Setter
  private String messageName;

  public ProtoBufSchemaConfig() {
    super("ProtoBuf");
  }


  @Override
  protected JSONObject packData() {
    JSONObject data = new JSONObject();
    packData(data);
    data.put("descriptor", new String(Base64.getEncoder().encode(descriptor)));
    data.put("messageName", messageName);
    return data;
  }
}