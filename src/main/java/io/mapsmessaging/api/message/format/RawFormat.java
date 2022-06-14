package io.mapsmessaging.api.message.format;

import io.mapsmessaging.selector.IdentifierResolver;
import java.io.IOException;

public class RawFormat implements Format{

  @Override
  public String getName() {
    return "RAW";
  }

  @Override
  public String getDescription() {
    return "Processes byte[] payloads";
  }

  @Override
  public byte[] toByteArray(Object obj) throws IOException {
    if(obj instanceof byte[]){
      return (byte[]) obj;
    }
    return null;
  }

  @Override
  public Object fromByteArray(byte[] payload) throws IOException {
    return payload;
  }

  @Override
  public IdentifierResolver getResolver(byte[] payload) throws IOException {
    return null;
  }
}
