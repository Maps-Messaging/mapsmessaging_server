package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.transactions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.RequestHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.transactions.handler.TransactionHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class TransactionManager extends RequestHandler {

  public TransactionManager() {
    super(new JetStreamFrameHandler[]{
      new TransactionHandler()
    });
  }

  @Override
  public String getType() {
    return "$JS.ACK";
  }


  public NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException {
    for (JetStreamFrameHandler handler : handlers) {
      if (subject.startsWith(handler.getName())) {
        return handler.handle(frame, null, sessionState);
      }
    }
    return createError(sessionState.getJetStreamRequestManager().getJetSubject(),
        sessionState.getJetStreamRequestManager().getSid(frame.getReplyTo()),
        "Function not implemented: " + subject);
  }


}
