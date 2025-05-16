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
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class InfoHandler extends JetStreamFrameHandler {

  private static final String TYPE = "io.nats.jetstream.api.v1.consumer_info_response";

  @Override
  public String getName() {
    return "CONSUMER.INFO";
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
      return buildError(TYPE,"Invalid consumer info subject", replyTo, sessionState);
    }

    String stream = parts[4];
    String consumerName = parts[5];

    NamedConsumer consumer = sessionState.getNamedConsumers().get(consumerName);
    if (consumer == null) {
      return buildError(TYPE,"Consumer '" + consumerName + "' not found", replyTo, sessionState);
    }

    JsonObject response = new JsonObject();
    response.addProperty("type", TYPE);
    response.addProperty("stream_name", stream);
    response.addProperty("name", consumerName);
    response.addProperty("created", consumer.getCreated().toString());
    response.addProperty("num_ack_pending", consumer.getAckPendingCount());

    JsonObject configJson = gson.toJsonTree(consumer.getConfig()).getAsJsonObject();
    response.add("config", configJson);

    PayloadFrame success = new MsgFrame(0);
    success.setSubject(replyTo);
    success.setSubscriptionId(sessionState.getJetStreamRequestManager().getSid(replyTo));
    success.setPayload(gson.toJson(response).getBytes(StandardCharsets.UTF_8));
    return success;
  }

}
