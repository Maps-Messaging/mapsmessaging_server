package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers;

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class StreamMsgGetHandler extends JetStreamHandler {
  @Override
  public String getName() {
    return "MSG.GET";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    return null;
  }
}
