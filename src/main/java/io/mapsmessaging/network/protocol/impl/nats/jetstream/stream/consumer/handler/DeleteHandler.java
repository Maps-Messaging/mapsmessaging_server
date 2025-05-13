package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler;

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DeleteHandler extends JetStreamFrameHandler {

  private static final String TYPE = "io.nats.jetstream.api.v1.consumer_delete_response";

  @Override
  public String getName() {
    return "CONSUMER.DELETE";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();
    String replyTo = frame.getReplyTo();

    if (replyTo == null || replyTo.isEmpty()) {
      return buildError(TYPE,"Missing reply subject", replyTo, sessionState);
    }

    String[] parts = subject.split("\\.");
    if (parts.length < 6) {
      return buildError(TYPE,"Invalid consumer delete subject", replyTo, sessionState);
    }

    String stream = parts[4];
    String consumerName = parts[5];

    StreamInfoList streamInfoList = NamespaceManager.getInstance().getStream(stream);
    if (streamInfoList == null) {
      return buildError(TYPE,"Stream '" + stream + "' not found", replyTo, sessionState);
    }

    NamedConsumer namedConsumer = sessionState.getNamedConsumers().remove(consumerName);
    if (namedConsumer == null) {
      return buildError(TYPE,"Consumer '" + consumerName + "' not found", replyTo, sessionState);
    }

    sessionState.removeSubscription(consumerName);
    namedConsumer.getEvents().clear();
    namedConsumer.getStreams().clear();

    JsonObject response = new JsonObject();
    response.addProperty("type", TYPE);
    response.addProperty("stream", stream);
    response.addProperty("success", true);

    PayloadFrame success = new MsgFrame(0);
    success.setSubject(replyTo);
    success.setSubscriptionId(sessionState.getJetStreamRequestManager().getSid(replyTo));
    success.setPayload(gson.toJson(response).getBytes(StandardCharsets.UTF_8));
    return success;
  }
}
