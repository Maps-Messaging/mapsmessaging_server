package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler;

import com.google.gson.JsonObject;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.NextRequest;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class MessageHandler  extends JetStreamFrameHandler {
  private static final String TYPE = "io.nats.jetstream.api.v1.consumer_next_response";

  @Override
  public String getName() {
    return "CONSUMER.MSG.NEXT";
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
      return buildError(TYPE,"Invalid MSG.NEXT subject", replyTo, sessionState);
    }

    String stream = parts[4];
    String consumerName = parts[5];

    NamedConsumer consumer = sessionState.getNamedConsumers().get(consumerName);
    if (consumer == null) {
      return buildError(TYPE,"Consumer '" + consumerName + "' not found", replyTo, sessionState);
    }
    if(!consumer.getStreamName().equals(stream)){
      return buildError(TYPE,"Consumer '" + consumerName + "' not bound to stream "+stream, replyTo, sessionState);
    }
    NextRequest request = gson.fromJson(json, NextRequest.class);

    for (int i = 0; i < request.getBatch(); i++) {
      Message msg = consumer.getNextMessage();
      // Send message to the consumer

      if (msg != null) {
        // pack up the MsgFrame to send

      } else if (!request.isNo_wait()) {
        // TODO: enqueue expiration/delivery wait logic if needed

        break;
      }
    }

    return null; // already wrote frames directly
  }
}
