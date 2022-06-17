package io.mapsmessaging.api;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class Metrics extends Destination {

  Metrics(@NonNull @NotNull DestinationImpl impl) {
    super(impl);
  }

  @Override
  public int storeMessage(@NonNull @NotNull Message message) throws IOException {
    // No we don't store events for metrics
    return 0;
  }

  @Override
  public long getStoredMessages() throws IOException {
    return 0; // We have no persistent stored messages
  }

}