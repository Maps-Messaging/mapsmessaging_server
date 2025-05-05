package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamApiManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class JetStreamRequestManager {
  private final JetStreamApiManager jetStreamApiManager;

  public JetStreamRequestManager() {
    jetStreamApiManager = new JetStreamApiManager();
  }

  public boolean isJetStreamRequest(PayloadFrame frame) {
    String subject = frame.getSubject();
    return (subject != null && (
        subject.startsWith("$JS")
            || subject.startsWith("$KV")
            || subject.startsWith("$O")));
  }

  public NatsFrame process(PayloadFrame frame, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();

    if (subject.startsWith("$JS.API.")) {
      if (!sessionState.getProtocol().getNatsConfig().isEnableStreams()) {
        return new ErrFrame("Streams are disabled");
      }
      return jetStreamApiManager.process(subject, frame, sessionState);
    } else if (subject.startsWith("$KV.")) {
      if (!sessionState.getProtocol().getNatsConfig().isEnableKeyValues()) {
        return new ErrFrame("Key Values are disabled");
      }
      return new ErrFrame("KeyValue handler not yet implemented");

    } else if (subject.startsWith("$O.")) {
      if (!sessionState.getProtocol().getNatsConfig().isEnableObjectStore()) {
        return new ErrFrame("Object Store is disabled");
      }
      return new ErrFrame("ObjectStore handler not yet implemented");

    } else {
      return new ErrFrame("Unknown JetStream request: " + subject);
    }
  }

}
