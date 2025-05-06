package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers;


import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class StreamPurgeHandler extends JetStreamFrameHandler {
  @Override
  public String getName() {
    return "PURGE";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    return null;
  }
}
