package io.mapsmessaging.network.protocol.impl.plugin.api;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.selector.operators.ParserExecutor;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;

public class DestinationContext {

  private final Destination destination;

  protected DestinationContext(Destination destination) {
    this.destination = destination;
  }

  public int writeEvent(@NotNull Message msg, ParserExecutor parser) throws IOException {
    if(parser == null || parser.evaluate(msg)) {
      return destination.storeMessage(msg);
    }
    return 0;
  }

}
