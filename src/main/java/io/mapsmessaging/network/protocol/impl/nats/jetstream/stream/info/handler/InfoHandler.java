package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.info.handler;

import com.google.gson.JsonObject;
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class InfoHandler extends JetStreamFrameHandler {

  @Override
  public String getName() {
    return "INFO";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    NatsFrame response = super.buildResponse(frame.getSubject(), frame, sessionState);
    if (response instanceof ErrFrame) {
      return response;
    }
    ((PayloadFrame) response).setPayload(buildInfo().getBytes());
    return response;
  }

  private String buildInfo() {
    JsonObject root = new JsonObject();
    root.addProperty("type", "io.nats.jetstream.api.v1.info_response");
    root.addProperty("server_id", MessageDaemon.getInstance().getUuid().toString());
    root.addProperty("server_name", MessageDaemon.getInstance().getHostname());
    root.addProperty("version", BuildInfo.getBuildVersion());
    root.addProperty("jetstream", true);

    JsonObject config = new JsonObject();
    config.addProperty("api_prefix", "$JS.API");
    root.add("config", config);
    return gson.toJson(root);
  }

}
