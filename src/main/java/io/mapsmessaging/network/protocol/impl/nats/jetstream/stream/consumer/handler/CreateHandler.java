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
import java.util.stream.Collectors;

public class CreateHandler extends JetStreamFrameHandler {
  private static final String TYPE = "io.nats.jetstream.api.v1.consumer_create_response";
  @Override
  public String getName() {
    return "CONSUMER.CREATE";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String replyTo = frame.getReplyTo();
    if (replyTo == null || replyTo.isEmpty()) {
      return buildError(TYPE,"Missing reply subject", replyTo, sessionState);
    }

    String subject = frame.getSubject();
    NatsFrame msg = buildResponse(subject, frame, sessionState);
    if (msg instanceof ErrFrame) {
      return msg;
    }

    String[] parts = subject.split("\\.");
    if (parts.length < 5) {
      return buildError(TYPE,"Invalid consume create subject", replyTo, sessionState);
    }

    String stream = parts[4];
    StreamInfoList streamInfoList = NamespaceManager.getInstance().getStream(stream);
    if (streamInfoList == null) {
      return buildError(TYPE,"Stream '" + stream + "' not found", replyTo, sessionState);
    }

    String name = "";
    if(parts.length == 6){
      name = parts[5];
    }
    else{
      name = "_Ephemeral-" + UUID.randomUUID();
    }

    ConsumerConfig config = gson.fromJson(json, ConsumerConfig.class);
    List<StreamInfo> subjectList = streamInfoList.getSubjects();
    if (config.getFilterSubject() != null) {
      subjectList = filterSubjects(config.getFilterSubject(), subjectList);
      if (subjectList.isEmpty()) {
        return buildError(TYPE,"Invalid filter", replyTo, sessionState);
      }
    }
    // Register with the session state
    if(sessionState.contains(name)) {
      return buildError(TYPE,"Duplicate consumer name", replyTo, sessionState);
    }
    List<StreamSubscriptionInfo> subscriptionInfoList = new ArrayList<>();
    // Add the specific subscriptions
    for(StreamInfo streamInfo : subjectList) {
      ClientAcknowledgement acknowledgement = getClientAcknowledgement(config);
      SubscribedEventManager eventManager = sessionState.subscribe(streamInfo.getSubject(), name, null, acknowledgement, 1, true); // only allow 1 event at a time here
      StreamSubscriptionInfo streamSubscriptionInfo = new StreamSubscriptionInfo(eventManager, streamInfo);
      subscriptionInfoList.add(streamSubscriptionInfo);
    }
    NamedConsumer namedConsumer = new NamedConsumer(name, stream, config, subscriptionInfoList);
    config.setAckPolicy(AckPolicy.EXPLICIT);
    config.setReplayPolicy(ReplayPolicy.INSTANT);
    config.setDeliverPolicy(DeliverPolicy.ALL);
    config.setMaxDeliver(-1);
    config.setMaxAckPending(1000);
    sessionState.getNamedConsumers().put(name, namedConsumer);
    ConsumerCreateResponse createResponse = new ConsumerCreateResponse();
    createResponse.setName(name);
    createResponse.setStream_name(stream);
    createResponse.setDelivered(new ConsumerCreateResponse.DeliveryInfo(0, 0));
    createResponse.setAck_floor(new ConsumerCreateResponse.AckFloor(0, 0));

    createResponse.setCreated(namedConsumer.getCreated());
    createResponse.setTs(new Date(System.currentTimeMillis()).toInstant());
    config.setName(name);
    createResponse.setConfig(config);

    PayloadFrame payloadFrame = (PayloadFrame) msg;
    payloadFrame.setPayload((gson.toJson(createResponse)).getBytes(StandardCharsets.UTF_8));
    return msg;
  }

  private ClientAcknowledgement getClientAcknowledgement(ConsumerConfig config) {
    if(config.getAckPolicy() == null) return ClientAcknowledgement.AUTO;
    switch (config.getAckPolicy()){
      case NONE:
        return ClientAcknowledgement.AUTO;

      case ALL:
        return ClientAcknowledgement.BLOCK;

      case EXPLICIT:
        return ClientAcknowledgement.INDIVIDUAL;

      default:
        return ClientAcknowledgement.AUTO;
    }
  }

  private List<StreamInfo> filterSubjects(String filter, List<StreamInfo> subjectList) {
    return subjectList.stream().filter(streamInfo -> matchesFilter(streamInfo.getSubject(), filter)).collect(Collectors.toList());
  }

  private boolean matchesFilter(String subject, String filter) {
    if (filter == null) return true; // No filter means match all

    String[] subjectParts = subject.split("\\.");
    String[] filterParts = filter.split("\\.");

    int i = 0;
    while (i < filterParts.length) {
      String f = filterParts[i];

      if (f.equals(">")) {
        return true; // matches rest
      }

      if (i >= subjectParts.length) {
        return false;
      }

      if (!f.equals("*") && !f.equals(subjectParts[i])) {
        return false;
      }

      i++;
    }

    return i == subjectParts.length;
  }

}