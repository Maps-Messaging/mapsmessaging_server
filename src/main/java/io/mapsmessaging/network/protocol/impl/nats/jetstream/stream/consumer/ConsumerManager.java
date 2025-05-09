package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;

import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.RequestHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler.*;

public class ConsumerManager extends RequestHandler {

  public ConsumerManager() {
    super(new JetStreamFrameHandler[]{
        new CreateHandler(),
        new DeleteHandler(),
        new DurableCreateHandler(),
        new InfoHandler(),
        new ListHandler(),
        new MessageHandler(),
        new NamesHandler()
    });
  }

  @Override
  public String getType() {
    return "$JS.API.CONSUMER";
  }

}
