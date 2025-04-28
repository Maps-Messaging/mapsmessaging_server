package io.mapsmessaging.network.protocol.impl.nats;

import java.io.IOException;

public class NatsProtocolException extends IOException {

  public NatsProtocolException(String message) {
    super(message);
  }
}
