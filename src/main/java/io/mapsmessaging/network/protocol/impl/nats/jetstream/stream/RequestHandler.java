package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public abstract class RequestHandler extends BaseStreamApiHandler {

  protected final JetStreamFrameHandler[] handlers;

  protected RequestHandler(JetStreamFrameHandler[] handlers){
    this.handlers = handlers;
  }

  public NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException {
    String action = subject.substring("$JS.API.".length());
    byte[] data = frame.getPayload();
    JsonObject json = (data != null && data.length > 0) ? JsonParser.parseString(new String(data)).getAsJsonObject() : null;
    for (JetStreamFrameHandler handler : handlers) {
      if (action.startsWith(handler.getName())) {
        return handler.handle(frame, json, sessionState);
      }
    }
    return createError(sessionState.getJetStreamRequestManager().getJetSubject(),
        sessionState.getJetStreamRequestManager().getSid(frame.getReplyTo()),
        "Function not implemented: " + subject);
  }


  public abstract String getType();



  protected NatsFrame createError(String subject, String subscriptionId, String errorMsg) {
    MsgFrame errorFrame = new MsgFrame(0);
    errorFrame.setSubject(subject);
    errorFrame.setSubscriptionId(subscriptionId);
    JsonObject root = new JsonObject();
    root.addProperty("type", "io.nats.jetstream.api.v1.error");

    JsonObject error = new JsonObject();
    error.addProperty("code", 501);
    error.addProperty("err_code", 1);
    error.addProperty("description", errorMsg);

    root.add("error", error);
    errorFrame.setPayload(gson.toJson(root).getBytes());
    return errorFrame;
  }

}
