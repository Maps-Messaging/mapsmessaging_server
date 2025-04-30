package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class JetStreamRequestManager {


  public JetStreamRequestManager() {}

  public boolean isJetStreamRequest(PayloadFrame frame) {
    String subject = frame.getSubject();
    return (subject != null && (
        subject.startsWith("$JS") // Stream Requests
    || subject.startsWith("$KV")  // Key Value Requesus
    || subject.startsWith("$O"))); // Object stores
  }

  public NatsFrame process(PayloadFrame frame, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();
    String[] request = subject.split("\\.");
    switch(request[0]){
      case "$JS":
        break;
      case "$KV":
        break;
      case "$O":
        break;

      default:
        ErrFrame errFrame = new ErrFrame();
        errFrame.setError("Unknown jetstream request "+request[0]);
        return errFrame;
    }
    return null;
  }
}
