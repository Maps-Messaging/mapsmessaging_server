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
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamArchiveManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamSubscriptionInitializer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.DurableConsumerSession;
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

  private static final String CONFIG_KEY = "config";

  private static final String TYPE = "io.nats.jetstream.api.v1.consumer_create_response";

  @Override
  public String getName() {
    return "CONSUMER.CREATE";
  }

  @Override
  @SuppressWarnings("java:S2445") // Coordinate consumer registration with the synchronized SessionState operations and deletion handler.
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

    ConsumerConfig config;
    try {
      config = parseConfig(json);
    } catch (IllegalArgumentException invalid) {
      return buildError(TYPE, 400, invalid.getMessage(), replyTo, sessionState);
    }
    applyDefaults(config);
    String name = parts.length == 6 ? parts[5] : consumerName(config);
    config.setName(name);
    String validationError = validateConfig(config, name);
    if (validationError != null) {
      return buildError(TYPE, 400, validationError, replyTo, sessionState);
    }
    List<StreamInfo> subjects = selectSubjects(streamInfoList, config);
    if (hasSubjectFilter(config) && subjects.isEmpty()) {
      return buildError(TYPE, "Invalid filter subject", replyTo, sessionState);
    }

    NamedConsumer consumer;
    List<StreamSubscriptionInfo> registered = new ArrayList<>();
    Session consumerSession = null;
    synchronized (sessionState) {
      if (sessionState.contains(name)) {
        return buildError(TYPE, "Duplicate consumer name", replyTo, sessionState);
      }
      try {
        consumerSession = isDurable(config)
            ? DurableConsumerSession.open(sessionState, stream, name)
            : DurableConsumerSession.openEphemeral(sessionState);
        consumer = registerConsumer(sessionState, consumerSession, stream, name, config, subjects, registered);
      } catch (IOException | RuntimeException exception) {
        cleanupFailedConsumer(sessionState, consumerSession, registered);
        return buildError(TYPE, 500, "Failed to register consumer: " + exception.getMessage(), replyTo, sessionState);
      }
    }
    return populateResponse(response, consumer, config, stream, name);
  }

  private static boolean isDurable(ConsumerConfig config) {
    return config.getDurableName() != null && !config.getDurableName().isBlank();
  }

  private static String validateConfig(ConsumerConfig config, String name) {
    if (isDurable(config) && !name.equals(config.getDurableName())) {
      return "Durable name must match the consumer name";
    }
    // The initial archive-copy operation currently supports the all/new policies.
    // Do not silently replay ALL when the client requested LAST or a start position.
    if (config.getDeliverPolicy() != DeliverPolicy.ALL && config.getDeliverPolicy() != DeliverPolicy.NEW) {
      return "JetStream delivery policy is not implemented: " + config.getDeliverPolicy();
    }
    return null;
  }

  private static boolean hasSubjectFilter(ConsumerConfig config) {
    return config.getFilterSubject() != null && !config.getFilterSubject().isBlank();
  }

  private static List<StreamInfo> selectSubjects(StreamInfoList streamInfoList, ConsumerConfig config) {
    List<StreamInfo> subjects = streamInfoList.getSubjects();
    if (hasSubjectFilter(config)) {
      return subjects.stream().filter(info -> matchesFilter(info.getSubject(), config.getFilterSubject())).toList();
    }
    return subjects;
  }

  private NamedConsumer registerConsumer(SessionState state, Session consumerSession, String stream, String name,
                                         ConsumerConfig config, List<StreamInfo> subjects,
                                         List<StreamSubscriptionInfo> registered) throws IOException {
    int index = 0;
    for (StreamInfo streamInfo : subjects) {
      String alias = "jetstream:" + stream + ":" + name + ":" + index++;
      registerSubject(state, consumerSession, stream, config, streamInfo, alias, registered);
    }
    NamedConsumer consumer = new NamedConsumer(name, stream, config, registered);
    if (!state.addNamedConsumer(consumer)) {
      throw new IOException("Duplicate consumer name");
    }
    consumer.attachMapsSession(consumerSession);
    return consumer;
  }

  private void registerSubject(SessionState state, Session consumerSession, String stream, ConsumerConfig config,
                               StreamInfo streamInfo, String alias,
                               List<StreamSubscriptionInfo> registered) throws IOException {
    String mapsTopic = streamInfo.getDestination().getFullyQualifiedNamespace();
    String archiveId = JetStreamArchiveManager.getInstance().ensureArchive(state, stream, mapsTopic);
    boolean resumed = isDurable(config) && consumerSession.isRestored()
        && streamInfo.getDestination().getSubscription(consumerSession.getName()) != null;
    SubscribedEventManager manager = subscribeConsumerSubject(consumerSession, config, streamInfo, stream, alias);
    if (manager == null) {
      throw new IOException("Unable to register consumer subscription " + streamInfo.getSubject());
    }
    registered.add(new StreamSubscriptionInfo(manager, streamInfo, alias));
    if (!resumed && config.getDeliverPolicy() == DeliverPolicy.ALL) {
      JetStreamSubscriptionInitializer.seedFromArchive(
          streamInfo.getDestination(), archiveId, consumerSession.getName(), config.getFilterSubject());
    }
  }

  private NatsFrame populateResponse(NatsFrame response, NamedConsumer consumer, ConsumerConfig config,
                                     String stream, String name) {
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

  private void cleanupFailedConsumer(SessionState state, Session consumerSession,
                                     List<StreamSubscriptionInfo> registered) {
    if (consumerSession == null) {
      return;
    }
    if (!consumerSession.isRestored()) {
      for (StreamSubscriptionInfo subscription : registered) {
        NatsJetStreamSubscriptions.remove(consumerSession, subscription.getSubscribedEventManager());
      }
    }
    DurableConsumerSession.close(state, consumerSession);
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

  SubscribedEventManager subscribeConsumerSubject(Session consumerSession, ConsumerConfig config, StreamInfo streamInfo,
                                                   String streamName, String alias) throws IOException {
    ClientAcknowledgement acknowledgement = getClientAcknowledgement(config);
    String subject = streamName + "." + streamInfo.getSubject().replace('/', '.');
    return DurableConsumerSession.subscribe(consumerSession, subject, alias, acknowledgement, 1);
  }

  ConsumerConfig parseConfig(JsonObject request) {
    JsonObject config = request != null && request.has(CONFIG_KEY) && request.get(CONFIG_KEY).isJsonObject()
        ? request.getAsJsonObject(CONFIG_KEY) : request;
    if (config == null) {
      return new ConsumerConfig();
    }
    ConsumerConfig consumerConfig = gson.fromJson(config, ConsumerConfig.class);
    if (consumerConfig != null && consumerConfig.getDeliverPolicy() == null
        && config.has("deliver_policy") && !config.get("deliver_policy").isJsonNull()) {
      throw new IllegalArgumentException("Unknown JetStream delivery policy");
    }
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
    String[] subjectTokens = subject.replace('/', '.').split("\\.", -1);
    String[] filterTokens = filter.split("\\.", -1);
    int position = 0;
    while (position < subjectTokens.length && position < filterTokens.length) {
      String source = subjectTokens[position];
      String requested = filterTokens[position];
      if (source.isEmpty() || requested.isEmpty()) {
        return false;
      }
      if (">".equals(source)) {
        return position == subjectTokens.length - 1;
      }
      if (">".equals(requested)) {
        return position == filterTokens.length - 1;
      }
      if (!"*".equals(source) && !"*".equals(requested) && !source.equals(requested)) {
        return false;
      }
      position++;
    }
    return position == subjectTokens.length && position == filterTokens.length;
  }
}
