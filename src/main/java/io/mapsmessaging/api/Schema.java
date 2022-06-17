package io.mapsmessaging.api;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.configuration.JsonParser;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class Schema extends Destination {

  Schema(@NonNull @NotNull DestinationImpl impl) {
    super(impl);
  }


  @Override
  public int storeMessage(@NonNull @NotNull Message message) throws IOException {
    // No we don't store events we need to parse the message to change this destinations schema
    try {
      JSONObject schemaRequest = new JSONObject(message.getOpaqueData());
      if(schemaRequest.has("schema")){
        JsonParser parser = new JsonParser(schemaRequest.getJSONObject("schema"));
        ConfigurationProperties props = new ConfigurationProperties(parser.parse());
        io.mapsmessaging.engine.schema.Schema schema = new io.mapsmessaging.engine.schema.Schema(props);
        destinationImpl.getSchema().update(schema);
      }
    } catch (Exception e) {
      throw new IOException("Invalid schema format");
    }
    return 1;
  }

  @Override
  public long getStoredMessages() throws IOException {
    return 1; // We only have 1 schema per destination
  }

}