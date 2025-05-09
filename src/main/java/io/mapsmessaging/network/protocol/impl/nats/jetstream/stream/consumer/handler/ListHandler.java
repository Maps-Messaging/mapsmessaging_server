package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends JetStreamFrameHandler {

  private static final String TYPE = "io.nats.jetstream.api.v1.consumer_list_response";

  @Override
  public String getName() {
    return "CONSUMER.LIST";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();
    String replyTo = frame.getReplyTo();

    if (replyTo == null || replyTo.isEmpty()) {
      return buildError(TYPE, "Missing reply subject", replyTo, sessionState);
    }

    String[] parts = subject.split("\\.");
    if (parts.length < 5) {
      return buildError(TYPE, "Invalid consumer list subject", replyTo, sessionState);
    }

    String stream = parts[4];
    int offset = json.has("offset") ? json.get("offset").getAsInt() : 0;
    int limit = json.has("limit") ? json.get("limit").getAsInt() : 1024;

    List<NamedConsumer> all = sessionState.getNamedConsumers().values().stream()
        .filter(c -> stream.equalsIgnoreCase(c.getStreamName()))
        .collect(Collectors.toList());

    int total = all.size();
    offset = Math.max(0, Math.min(offset, total));
    int toIndex = Math.min(offset + limit, total);
    List<NamedConsumer> page = all.subList(offset, toIndex);

    JsonArray consumers = new JsonArray();
    for (NamedConsumer c : page) {
      JsonObject entry = new JsonObject();
      entry.addProperty("stream_name", stream);
      entry.addProperty("name", c.getName());
      entry.addProperty("created", c.getCreated().toString());
      entry.addProperty("num_ack_pending", c.getAckPendingCount());
      entry.add("config", gson.toJsonTree(c.getConfig()));
      consumers.add(entry);
    }

    JsonObject response = new JsonObject();
    response.addProperty("type", TYPE);
    response.add("consumers", consumers);
    response.addProperty("total", total);
    response.addProperty("offset", offset);
    response.addProperty("limit", limit);

    PayloadFrame success = new MsgFrame(0);
    success.setSubject(replyTo);
    success.setSubscriptionId(sessionState.getJetStreamRequestManager().getSubscriptionId());
    success.setPayload(gson.toJson(response).getBytes(StandardCharsets.UTF_8));
    return success;
  }
}
