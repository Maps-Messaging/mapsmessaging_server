package io.mapsmessaging.rest.data;

import io.mapsmessaging.engine.destination.DestinationImpl;
import java.io.IOException;
import lombok.Getter;

public class Destination {

  @Getter
  private final String name;

  @Getter
  private final String type;

  @Getter
  private final long storedMessages;

  @Getter
  private final long delayedMessages;

  @Getter
  private final long pendingMessages;

  @Getter
  private final String schemaId;

  public Destination(DestinationImpl destinationImpl) throws IOException {
    this.name = destinationImpl.getFullyQualifiedNamespace();
    storedMessages = destinationImpl.getStoredMessages();
    type = destinationImpl.getResourceType().getName();
    schemaId = destinationImpl.getSchema().getUniqueId();
    delayedMessages = destinationImpl.getDelayedMessages();
    pendingMessages = destinationImpl.getPendingTransactions();
  }
}
