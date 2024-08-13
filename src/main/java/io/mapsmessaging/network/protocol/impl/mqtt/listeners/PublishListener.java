/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.*;
import io.mapsmessaging.selector.operators.ParserExecutor;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;

public class PublishListener extends PacketListener {

  public static Message createMessage(byte[] msg, Priority priority, boolean retain, QualityOfService qos, ProtocolMessageTransformation transformation, Transformer transformer, String schemaId) {
    HashMap<String, String> meta = new LinkedHashMap<>();
    meta.put("protocol", "MQTT");
    meta.put("version", "4");

    HashMap<String, TypedData> dataHashMap = new LinkedHashMap<>();
    MessageBuilder mb = new MessageBuilder();
    return mb.setDataMap(dataHashMap)
        .setPriority(priority)
        .setRetain(retain)
        .setOpaqueData(msg)
        .setMeta(meta)
        .setQoS(qos)
        .setSchemaId(schemaId)
        .storeOffline(qos.isStoreOffLine())
        .setTransformation(transformation)
        .setDestinationTransformer(transformer)
        .build();
  }

  private MQTTPacket getResponse(Publish publish){
    if (publish.getQos().equals(QualityOfService.AT_LEAST_ONCE)) {
      return new PubAck(publish.getPacketId());
    } else if (publish.getQos().equals(QualityOfService.EXACTLY_ONCE)) {
      return new PubRec(publish.getPacketId());
    }
    return null;
  }

  private String parseForLookup(ProtocolImpl protocol, Publish publish){
    String lookup = publish.getDestinationName();

    Map<String, String> map = ((MQTTProtocol) protocol).getTopicNameMapping();
    if (map != null) {
      lookup = map.get(publish.getDestinationName());
      if (lookup == null) {
        lookup = publish.getDestinationName();
        for(Map.Entry<String, String> remote:map.entrySet()){
          if(remote.getKey().endsWith("#")){
            String check = remote.getValue();
            String tmp = remote.getKey().substring(0, remote.getKey().length()-1);
            if(lookup.startsWith(tmp)){
              if (lookup.toLowerCase().startsWith(DestinationMode.SCHEMA.getNamespace())) {
                lookup = lookup.substring(DestinationMode.SCHEMA.getNamespace().length());
              }
              lookup = check + lookup;
              lookup = lookup.replace("#", "");
              lookup = lookup.replaceAll("//", "/");
            }
          }
        }
      }
    }
    return lookup;
  }

  @SneakyThrows
  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) throws MalformedException {
    checkState(session);

    Publish publish = (Publish) mqttPacket;
    MQTTPacket response = getResponse(publish);
    String lookup =parseForLookup(protocol, publish);

    if (!lookup.startsWith("$") || publish.getDestinationName().toLowerCase().startsWith(DestinationMode.SCHEMA.getNamespace())) {
      processValidDestinations(publish, session, lookup, protocol, response, endPoint);
    } else {
      return response;
    }
    return null;
  }

  private void processValidDestinations(Publish publish, Session session, String lookup, ProtocolImpl protocol, MQTTPacket response, EndPoint endPoint)
      throws ExecutionException, InterruptedException {
    CompletableFuture<Destination> future = session.findDestination(lookup, DestinationType.TOPIC);
    future.thenApply(destination -> {
      if (destination != null) {
        try {
          processMessage(publish, protocol, session, response, destination);
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


  private void processMessage(Publish publish, ProtocolImpl protocol, Session session, MQTTPacket response, Destination destination) throws IOException {
    Transformer transformer = protocol.destinationTransformationLookup(destination.getFullyQualifiedNamespace());
    Message message = createMessage(
        publish.getPayload(),
        publish.getPriority(),
        publish.isRetain(),
        publish.getQos(),
        protocol.getTransformation(),
        transformer,
        destination.getSchema().getUniqueId()
    );
    ParserExecutor parserExecutor = protocol.getParser(publish.getDestinationName());
    if(parserExecutor != null && !parserExecutor.evaluate(message)){
      return;
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
