package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
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
    byte[] data = frame.getPayload();
    JsonObject json = data != null ? JsonParser.parseString(new String(data)).getAsJsonObject() : null;

    for (JetStreamHandler handler : handlers) {
      if (action.startsWith(handler.getName())) {
        return handler.handle(frame, json, sessionState);
      }
    }
    return createError(sessionState.getJetStreamRequestManager().getJetSubject(),
        sessionState.getJetStreamRequestManager().getSubscriptionId(),
        "Function not implemented: " + subject);
  }

  private NatsFrame createError(String subject, String subscriptionId, String errorMsg) {
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
    errorFrame.setPayload(new Gson().toJson(root).getBytes());
    return errorFrame;
  }

  @Override
  public String getType() {
    return "$JS.API.STREAM.";
  }

}
