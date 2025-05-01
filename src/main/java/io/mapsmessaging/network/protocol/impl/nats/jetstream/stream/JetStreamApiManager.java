package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class JetStreamApiManager {

  private final Manager[] managers;

  public JetStreamApiManager() {
    this.managers = new Manager[] {
        new StreamManager(),
        new ConsumerManager(),
        new InfoManager()
    };

  }

  public NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException {
    for(Manager manager : managers) {
      if(subject.startsWith(manager.getType())) {
        return manager.process(subject, frame, sessionState);
      }
    }
    return new ErrFrame("Unknown JetStream API call: " + subject);
  }

}
