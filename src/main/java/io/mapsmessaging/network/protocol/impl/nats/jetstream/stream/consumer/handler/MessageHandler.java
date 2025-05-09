package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler;

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class MessageHandler  extends JetStreamFrameHandler {

  @Override
  public String getName() {
    return "CONSUMER.MSG.NEXT";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    return null;
  }

}