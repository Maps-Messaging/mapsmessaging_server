package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers.*;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class StreamManager implements Manager {

  private final JetStreamHandler[] handlers = new JetStreamHandler[]{
      new StreamCreateHandler(),
      new StreamInfoHandler(),
      new StreamDeleteHandler(),
      new StreamListHandler(),
      new StreamUpdateHandler(),
      new StreamPurgeHandler(),
      new StreamMsgGetHandler(),
      new StreamMsgDeleteHandler(),
      new StreamNamesHandler()
  };

  public NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException {
    String action = subject.substring("$JS.API.STREAM.".length());
    for (JetStreamHandler handler : handlers) {
      if (action.startsWith(handler.getName())) {
        return handler.handle(frame, sessionState);
      }
    }
    return new ErrFrame("Unknown STREAM command: " + subject);
  }

  @Override
  public String getType() {
    return "$JS.API.STREAM.";
  }

}
