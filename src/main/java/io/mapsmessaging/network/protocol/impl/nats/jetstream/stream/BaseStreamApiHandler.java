package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.time.Instant;

public class BaseStreamApiHandler {

  protected final Gson gson;

  public BaseStreamApiHandler() {
    gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantAdapter())
        .create();
  }

  protected NatsFrame buildResponse(String subject, PayloadFrame payloadFrame, SessionState sessionState) {
    String replyTo = payloadFrame.getReplyTo();
    if (replyTo == null || replyTo.isEmpty()) {
      return new ErrFrame("Missing reply subject");
    }

    MsgFrame msg = new MsgFrame(0);
    msg.setSubject(replyTo);
    msg.setSubscriptionId(sessionState.getJetStreamRequestManager().getSid(replyTo));
    return msg;
  }
}
