package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class JetStreamFrameHandler extends BaseStreamApiHandler {
  public abstract String getName();

  public abstract NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException;

  protected String streamNotFound(String type) {
    return "{\n" +
        "  \"type\": \"" + type + "\",\n" +
        "  \"error\": {\n" +
        "    \"code\": 404,\n" +
        "    \"description\": \"stream not found\"\n" +
        "  }\n" +
        "}";
  }


  protected NatsFrame buildError(String type, String message, String replyTo, SessionState sessionState) {
    JsonObject err = new JsonObject();
    err.addProperty("code", 404);
    err.addProperty("description", message);

    JsonObject root = new JsonObject();
    root.addProperty("type", type);
    root.add("error", err);

    PayloadFrame frame = new MsgFrame(0);
    frame.setSubject(replyTo);
    frame.setSubscriptionId(sessionState.getJetStreamRequestManager().getSid(replyTo));
    frame.setPayload(gson.toJson(root).getBytes(StandardCharsets.UTF_8));
    return frame;
  }

}
