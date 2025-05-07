package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.info;

import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.RequestHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.info.handler.InfoHandler;

public class InfoManager extends RequestHandler {
  public InfoManager() {
    super(new JetStreamFrameHandler[]{new InfoHandler()});
  }

  @Override
  public String getType() {
    return "$JS.API.INFO";
  }


}
