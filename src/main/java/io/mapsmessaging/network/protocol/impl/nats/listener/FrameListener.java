package io.mapsmessaging.network.protocol.impl.nats.listener;


import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public interface FrameListener {

  void frameEvent(NatsFrame frame, SessionState engine, boolean endOfBuffer) throws IOException;

  default void postFrameHandling(NatsFrame frame, SessionState engine) {
    frame.complete();
  }


  default String convertSubject(String subject) {
    return subject
        .replace('.', '/')
        .replace('*', '+')
        .replace('>', '#');
  }
}
