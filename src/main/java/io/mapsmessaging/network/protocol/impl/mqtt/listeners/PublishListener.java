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

package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import io.mapsmessaging.analytics.Analyser;
import io.mapsmessaging.analytics.AnalyserFactory;
import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.*;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.selector.operators.ParserExecutor;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PublishListener extends PacketListener {

  public static Message createMessage(byte[] msg, Priority priority, boolean retain, QualityOfService qos, ProtocolMessageTransformation transformation, Protocol protocol) {
    HashMap<String, String> meta = new LinkedHashMap<>();
    meta.put("protocol", "MQTT");
    meta.put("version", "4");
    meta.put("sessionId", protocol.getSessionId());
    meta.put("time_ms", "" + System.currentTimeMillis());

    HashMap<String, TypedData> dataHashMap = new LinkedHashMap<>();
    MessageBuilder mb = new MessageBuilder();
    mb.setDataMap(dataHashMap)
        .setPriority(priority)
        .setRetain(retain)
        .setOpaqueData(msg)
        .setMeta(meta)
        .setQoS(qos)
        .storeOffline(qos.isStoreOffLine())
        .setTransformation(transformation);
    return MessageOverrides.createMessageBuilder(protocol.getProtocolConfig().getMessageDefaults(), mb).build();
  }

  private MQTTPacket getResponse(Publish publish){
    if (publish.getQos().equals(QualityOfService.AT_LEAST_ONCE)) {
      return new PubAck(publish.getPacketId());
    } else if (publish.getQos().equals(QualityOfService.EXACTLY_ONCE)) {
      return new PubRec(publish.getPacketId());
    }
    return null;
  }

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, Protocol protocol) throws MalformedException {
    checkState(session);

    Publish publish = (Publish) mqttPacket;
    MQTTPacket response = getResponse(publish);
    String lookup = protocol.parseForLookup(publish.getDestinationName());

    if (!lookup.startsWith("$") || publish.getDestinationName().toLowerCase().startsWith(DestinationMode.SCHEMA.getNamespace())) {
      try {
        processValidDestinations(publish, session, lookup, protocol, response, endPoint);
      } catch (ExecutionException e) {
        try {
          protocol.close();
        } catch (IOException ex) {
          //
        }
      } catch (InterruptedException e) {
        //ignore
      }
    } else {
      return response;
    }
    return null;
  }

  private void processValidDestinations(Publish publish, Session session, String lookup, Protocol protocol, MQTTPacket response, EndPoint endPoint)
      throws ExecutionException, InterruptedException {
    Message message = createMessage(
        publish.getPayload(),
        publish.getPriority(),
        publish.isRetain(),
        publish.getQos(),
        protocol.getProtocolMessageTransformation(),
        protocol
    );
    CompletableFuture<Destination> future = session.findDestination(lookup, DestinationType.TOPIC);
    future.thenApply(destination -> {
      if (destination != null) {
        try {
          processMessage(message, publish, protocol, session, response, destination, lookup);
          if (response != null) {
            ((MQTTProtocol) protocol).writeFrame(response);
          }
        } catch (IOException e) {
          logger.log(ServerLogMessages.MQTT_PUBLISH_STORE_FAILED, e);
          try {
            endPoint.close();
          } catch (IOException ioException) {
            // Ignore we are in an error state
          }
          future.completeExceptionally(new MalformedException("[MQTT-3.3.5-2]"));
        }
      }
      return destination;
    });
    future.get();
  }


  private void processMessage(Message message, Publish publish, Protocol protocol, Session session, MQTTPacket response, Destination destination, String topicName) throws IOException {
    Analyser analyser = protocol.getTopicNameAnalyserMap().get(topicName);
    if(analyser == null && !protocol.getResourceNameAnalyserMap().isEmpty()){
      for(Map.Entry<String, StatisticsConfigDTO> entry:protocol.getResourceNameAnalyserMap().entrySet()){
        if(DestinationSet.matches(entry.getKey(), topicName)){
          analyser = AnalyserFactory.getInstance().getAnalyser(entry.getValue());
          protocol.getTopicNameAnalyserMap().put(topicName, analyser);
          break;
        }
      }
    }

    ParserExecutor parserExecutor = protocol.getParser(topicName);
    if(parserExecutor != null && !parserExecutor.evaluate(message)){
      return;
    }
    if(analyser != null){
      message = analyser.ingest(message);
      if(message == null){
        return;
      }
    }


    if (response != null) {
      Transaction transaction = null;
      try {
        transaction = session.startTransaction(session.getName() + "_" + publish.getPacketId());
      } catch (TransactionException e) {
        logger.log(ServerLogMessages.MQTT_DUPLICATE_EVENT_RECEIVED, publish.getPacketId());
      }
      if (transaction != null) {
        transaction.add(destination, message);
        if (publish.getQos().equals(QualityOfService.AT_LEAST_ONCE)) {
          transaction.commit();
          session.closeTransaction(transaction);
        }
      }
    } else {
      destination.storeMessage(message);
    }
  }
}
