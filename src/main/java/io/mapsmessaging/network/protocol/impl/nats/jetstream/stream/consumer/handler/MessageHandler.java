/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler;

import com.google.gson.JsonObject;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.TransactionSubject;
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

    String stream = parts[5];
    String consumerName = parts[6];

    NamedConsumer consumer = sessionState.getNamedConsumers().get(consumerName);
    if (consumer == null) {
      return buildError(TYPE,"Consumer '" + consumerName + "' not found", replyTo, sessionState);
    }
    if(!consumer.getStreamName().equals(stream)){
      return buildError(TYPE,"Consumer '" + consumerName + "' not bound to stream "+stream, replyTo, sessionState);
    }
    NextRequest request = gson.fromJson(json, NextRequest.class);

    for (int i = 0; i < request.getBatch(); i++) {
      MessageEvent msg = consumer.getNextMessage();
      if (msg != null) {
        PayloadFrame payloadFrame = sessionState.buildPayloadFrame(msg.getMessage(), msg.getDestinationName());
        TransactionSubject replyToSubject = new TransactionSubject(consumer, msg.getDestinationName(), msg.getMessage().getIdentifier());

        payloadFrame.setReplyTo(replyToSubject.toSubject());
        payloadFrame.setSubscriptionId(sessionState.getJetStreamRequestManager().getSid(replyTo));
        sessionState.send(payloadFrame);
      } else if (!request.isNo_wait()) {
        break;
      }
    }
    return null;
  }
}
