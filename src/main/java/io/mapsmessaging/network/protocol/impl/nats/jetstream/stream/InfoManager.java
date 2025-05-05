package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class InfoManager implements Manager {
  @Override
  public String getType() {
    return "$JS.API.INFO";
  }

  @Override
  public NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException {
    String replyTo = frame.getReplyTo();
    if (replyTo == null || replyTo.isEmpty()) {
      return new ErrFrame("Missing reply subject");
    }

    MsgFrame msg = new MsgFrame(0);
    msg.setSubject(replyTo);
    msg.setSubscriptionId(sessionState.getJetStreamRequestManager().getSubscriptionId());
    msg.setPayload(buildInfo(sessionState).getBytes());
    return msg;
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
