package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class ConsumerManager implements Manager {

  @Override
  public String getType() {
    return "$JS.API.CONSUMER";
  }

  @Override
  public NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException {
    return null;
  }

}
