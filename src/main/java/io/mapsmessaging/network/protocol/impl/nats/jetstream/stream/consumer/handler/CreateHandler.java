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
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.*;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfo;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CreateHandler extends JetStreamFrameHandler {

  private static final String TYPE = "io.nats.jetstream.api.v1.consumer_create_response";

  @Override
  public String getName() {
    return "CONSUMER.CREATE";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String replyTo = frame.getReplyTo();
    if (replyTo == null || replyTo.isBlank()) {
      return new ErrFrame("Missing reply subject");
    }

    NatsFrame response = buildResponse(frame.getSubject(), frame, sessionState);
    if (response instanceof ErrFrame) {
      return response;
    }

    String[] parts = frame.getSubject().split("\\.");
    if (parts.length < 5 || parts.length > 6) {
      return buildError(TYPE, "Invalid consumer create subject", replyTo, sessionState);
    }
    String stream = parts[4];
    StreamInfoList streamInfoList = NamespaceManager.getInstance().getStream(stream);
    if (streamInfoList == null) {
      return buildError(TYPE, "Stream '" + stream + "' not found", replyTo, sessionState);
    }

    ConsumerConfig config = parseConfig(json);
    applyDefaults(config);
    String name = parts.length == 6 ? parts[5] : consumerName(config);
    config.setName(name);

    List<StreamInfo> subjects = streamInfoList.getSubjects();
    if (config.getFilterSubject() != null && !config.getFilterSubject().isBlank()) {
      subjects = subjects.stream().filter(info -> matchesFilter(info.getSubject(), config.getFilterSubject())).toList();
      if (subjects.isEmpty()) {
        return buildError(TYPE, "Invalid filter subject", replyTo, sessionState);
      }
    }

    NamedConsumer consumer;
    List<StreamSubscriptionInfo> registered = new ArrayList<>();
    synchronized (sessionState) {
      if (sessionState.contains(name)) {
        return buildError(TYPE, "Duplicate consumer name", replyTo, sessionState);
      }
      try {
        int index = 0;
        for (StreamInfo streamInfo : subjects) {
          String subscriptionAlias = "jetstream:" + stream + ":" + name + ":" + index++;
          SubscribedEventManager manager = subscribeConsumerSubject(sessionState, config, streamInfo, stream, subscriptionAlias);
          if (manager == null) {
            releaseSubscriptions(sessionState, registered);
            return buildError(TYPE, "Unable to register consumer subscription", replyTo, sessionState);
          }
          registered.add(new StreamSubscriptionInfo(manager, streamInfo, subscriptionAlias));
        }
        consumer = new NamedConsumer(name, stream, config, registered);
        if (!sessionState.addNamedConsumer(consumer)) {
          releaseSubscriptions(sessionState, registered);
          return buildError(TYPE, "Duplicate consumer name", replyTo, sessionState);
        }
      } catch (IOException | RuntimeException exception) {
        releaseSubscriptions(sessionState, registered);
        return buildError(TYPE, "Failed to register consumer: " + exception.getMessage(), replyTo, sessionState);
      }
    }

    ConsumerCreateResponse created = new ConsumerCreateResponse();
    created.setName(name);
    created.setStream_name(stream);
    created.setDelivered(new ConsumerCreateResponse.DeliveryInfo(0, 0));
    created.setAck_floor(new ConsumerCreateResponse.AckFloor(0, 0));
    created.setCreated(consumer.getCreated());
    created.setTs(new Date().toInstant());
    created.setConfig(config);
    ((PayloadFrame) response).setPayload(gson.toJson(created).getBytes(StandardCharsets.UTF_8));
    return response;
  }

  private void releaseSubscriptions(SessionState sessionState, List<StreamSubscriptionInfo> registered) {
    for (StreamSubscriptionInfo info : registered) {
      sessionState.removeSubscription(info.getSubscriptionAlias());
    }
  }

  private static String consumerName(ConsumerConfig config) {
    if (config.getDurableName() != null && !config.getDurableName().isBlank()) {
      return config.getDurableName();
    }
    if (config.getName() != null && !config.getName().isBlank()) {
      return config.getName();
    }
    return "_Ephemeral-" + UUID.randomUUID();
  }

  SubscribedEventManager subscribeConsumerSubject(SessionState sessionState, ConsumerConfig config, StreamInfo streamInfo,
                                                   String streamName, String alias) throws IOException {
    ClientAcknowledgement acknowledgement = getClientAcknowledgement(config);
    String subject = streamName + "." + streamInfo.getSubject().replace('/', '.');
    return sessionState.subscribeJetStream(subject, alias, acknowledgement, 1);
  }

  ConsumerConfig parseConfig(JsonObject request) {
    JsonObject config = request != null && request.has("config") && request.get("config").isJsonObject()
        ? request.getAsJsonObject("config") : request;
    if (config == null) {
      return new ConsumerConfig();
    }
    ConsumerConfig consumerConfig = gson.fromJson(config, ConsumerConfig.class);
    return consumerConfig == null ? new ConsumerConfig() : consumerConfig;
  }

  static void applyDefaults(ConsumerConfig config) {
    if (config.getAckPolicy() == null) {
      config.setAckPolicy(AckPolicy.EXPLICIT);
    }
    if (config.getReplayPolicy() == null) {
      config.setReplayPolicy(ReplayPolicy.INSTANT);
    }
    if (config.getDeliverPolicy() == null) {
      config.setDeliverPolicy(DeliverPolicy.ALL);
    }
    if (config.getMaxDeliver() == 0) {
      config.setMaxDeliver(-1);
    }
    if (config.getMaxAckPending() == null) {
      config.setMaxAckPending(1000);
    }
  }

  private ClientAcknowledgement getClientAcknowledgement(ConsumerConfig config) {
    if (config.getAckPolicy() == null) {
      return ClientAcknowledgement.AUTO;
    }
    return switch (config.getAckPolicy()) {
      case NONE -> ClientAcknowledgement.AUTO;
      case ALL -> ClientAcknowledgement.BLOCK;
      case EXPLICIT -> ClientAcknowledgement.INDIVIDUAL;
    };
  }

  static boolean matchesFilter(String subject, String filter) {
    if (filter == null || filter.isBlank()) {
      return true;
    }
    String[] subjectTokens = subject.replace('/', '.').split("\\.");
    String[] filterTokens = filter.split("\\.");
    int position = 0;
    for (int tokenIndex = 0; tokenIndex < filterTokens.length; tokenIndex++) {
      String token = filterTokens[tokenIndex];
      if (">".equals(token)) {
        return tokenIndex == filterTokens.length - 1 && position < subjectTokens.length;
      }
      if (position >= subjectTokens.length || (!"*".equals(token) && !token.equals(subjectTokens[position]))) {
        return false;
      }
      position++;
    }
    return position == subjectTokens.length;
  }
}
