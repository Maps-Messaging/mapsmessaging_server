package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.StreamManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.ConsumerManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.info.InfoManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class JetStreamApiManager {

  private final RequestHandler[] managers;

  public JetStreamApiManager() {
    this.managers = new RequestHandler[]{
        new StreamManager(),
        new ConsumerManager(),
        new InfoManager()
    };

  }

  public NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException {
    for (RequestHandler manager : managers) {
      if (subject.startsWith(manager.getType())) {
        return manager.process(subject, frame, sessionState);
      }
    }
    return new ErrFrame("Unknown JetStream API call: " + subject);
  }

}
