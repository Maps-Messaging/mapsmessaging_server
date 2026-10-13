/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import io.mapsmessaging.api.NatsJetStreamSubscriptions;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.DurableConsumerSession;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DeleteHandler extends JetStreamFrameHandler {

  private static final String TYPE = "io.nats.jetstream.api.v1.consumer_delete_response";

  @Override
  public String getName() {
    return "CONSUMER.DELETE";
  }

  @Override
  @SuppressWarnings("java:S2445") // Coordinate consumer removal with the synchronized SessionState operations and creation handler.
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

    NamedConsumer namedConsumer = sessionState.getNamedConsumers().get(consumerName);
    if (namedConsumer == null || !stream.equals(namedConsumer.getStreamName())) {
      return buildError(TYPE,"Consumer '" + consumerName + "' not found", replyTo, sessionState);
    }

    synchronized (sessionState) {
      if (!sessionState.getNamedConsumers().remove(consumerName, namedConsumer)) {
        return buildError(TYPE, "Consumer '" + consumerName + "' no longer exists", replyTo, sessionState);
      }
      removeSubscriptions(sessionState, namedConsumer);
      namedConsumer.close();
      closeConsumerSession(sessionState, namedConsumer);
    }
    return buildSuccess(stream, replyTo, sessionState);
  }

  private void removeSubscriptions(SessionState state, NamedConsumer consumer) throws IOException {
    for (StreamSubscriptionInfo subscription : consumer.getStreams()) {
      if (subscription.getSubscriptionAlias() != null) {
        removeSubscription(state, consumer, subscription);
      }
    }
  }

  private void removeSubscription(SessionState state, NamedConsumer consumer,
                                  StreamSubscriptionInfo subscription) throws IOException {
    if (consumer.getMapsSession() != null) {
      NatsJetStreamSubscriptions.remove(consumer.getMapsSession(), subscription.getSubscribedEventManager());
    } else {
      state.removeSubscription(subscription.getSubscriptionAlias());
    }
  }

  private void closeConsumerSession(SessionState state, NamedConsumer consumer) throws IOException {
    if (consumer.getMapsSession() == null) {
      return;
    }
    try {
      DurableConsumerSession.close(state, consumer.getMapsSession()).get();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted deleting durable consumer", interrupted);
    } catch (java.util.concurrent.ExecutionException failure) {
      throw new IOException("Could not delete durable consumer", failure);
    }
  }

  private NatsFrame buildSuccess(String stream, String replyTo, SessionState sessionState) {

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
