package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

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

}
