package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers;

import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class StreamCreateHandler implements JetStreamHandler {

  @Override
  public String getName() {
    return "CREATE";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, SessionState sessionState) throws IOException {
    return null;
  }
}
