package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api;


import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.RequestHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.*;

public class StreamManager extends RequestHandler {

  public StreamManager() {
    super(new JetStreamFrameHandler[]{
        new StreamCreateHandler(),
        new StreamInfoHandler(),
        new StreamDeleteHandler(),
        new StreamListHandler(),
        new StreamUpdateHandler(),
        new StreamNamesHandler()
    });
  }

  @Override
  public String getType() {
    return "$JS.API.STREAM.";
  }

}
