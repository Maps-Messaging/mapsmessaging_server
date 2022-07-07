package io.mapsmessaging.engine.resources;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.tasks.BulkRemoveMessageTask;
import io.mapsmessaging.storage.ExpiredStorableHandler;
import java.io.IOException;
import java.util.Queue;
import lombok.Setter;

public class MessageExpiryHandler implements ExpiredStorableHandler {

  private @Setter DestinationImpl destination;

  public MessageExpiryHandler() {
  }

  public MessageExpiryHandler(DestinationImpl destination) {
    this.destination = destination;
  }

  @Override
  public void expired(Queue<Long> queue) throws IOException {
    destination.handleTask(new BulkRemoveMessageTask(destination, queue));
  }

}
