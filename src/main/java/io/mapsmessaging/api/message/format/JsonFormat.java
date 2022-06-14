package io.mapsmessaging.api.message.format;

import io.mapsmessaging.selector.IdentifierResolver;
import java.io.IOException;
import org.json.JSONObject;

public class JsonFormat implements Format{

  @Override
  public String getName() {
    return "JSON";
  }

  @Override
  public String getDescription() {
    return "Processes JSON formatted payloads";
  }

  @Override
  public byte[] toByteArray(Object obj) throws IOException {
    if(obj instanceof JSONObject){
      return ((JSONObject)obj).toString(2).getBytes();
    }
    return null;
  }

  @Override
  public Object fromByteArray(byte[] payload) throws IOException {
    String tmp = new String(payload);
    return new JSONObject(tmp);
  }


  @Override
  public IdentifierResolver getResolver(byte[] payload) throws IOException {
    return new JsonIdentifierResolver((JSONObject)fromByteArray(payload));
  }

  public static final class JsonIdentifierResolver implements IdentifierResolver{

    private final JSONObject jsonObject;

    public JsonIdentifierResolver(JSONObject jsonObject){
      this.jsonObject = jsonObject;
    }

    @Override
    public Object get(String s) {
      return jsonObject.get(s);
    }

    @Override
    public byte[] getOpaqueData() {
      return IdentifierResolver.super.getOpaqueData();
    }
  }
}
