package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.info;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.RequestHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class InfoManager extends RequestHandler {
  @Override
  public String getType() {
    return "$JS.API.INFO";
  }

  @Override
  public NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException {
    NatsFrame response = super.buildResponse(subject, frame, sessionState);
    if (response instanceof ErrFrame) {
      return response;
    }
    ((PayloadFrame) response).setPayload(buildInfo(sessionState).getBytes());
    return response;
  }

  private String buildInfo(SessionState sessionState) {
    JsonObject root = new JsonObject();
    root.addProperty("type", "io.nats.jetstream.api.v1.info_response");
    root.addProperty("server_id", MessageDaemon.getInstance().getUuid().toString());
    root.addProperty("server_name", MessageDaemon.getInstance().getHostname());
    root.addProperty("version", BuildInfo.getBuildVersion());
    root.addProperty("jetstream", true);

    JsonObject config = new JsonObject();
    config.addProperty("api_prefix", "$JS.API");
    root.add("config", config);
    return new Gson().toJson(root);
  }


}
