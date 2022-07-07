package io.mapsmessaging.api;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class Schema extends Destination {

  Schema(@NonNull @NotNull DestinationImpl impl) {
    super(impl);
  }


  @Override
  public int storeMessage(@NonNull @NotNull Message message) throws IOException {
    // No we don't store events we need to parse the message to change this destinations schema
    try {
      SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(message.getOpaqueData());
      destinationImpl.updateSchema(config, message);
    } catch (Exception e) {
      throw new IOException("Invalid schema format");
    }
    return 1;
  }

  @Override
  public long getStoredMessages() throws IOException {
    return 1; // We only have 1 schema per destination
  }

  @Override
  public String getFullyQualifiedNamespace() {
    return "$schema/" + super.getFullyQualifiedNamespace();
  }
}