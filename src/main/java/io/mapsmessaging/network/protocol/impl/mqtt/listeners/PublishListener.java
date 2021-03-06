/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.api.TransactionException;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.logging.LogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PubAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PubRec;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Publish;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PublishListener extends PacketListener {

  public static Message createMessage(byte[] msg, Priority priority, boolean retain, QualityOfService qos, ProtocolMessageTransformation transformation, Transformer transformer) {
    HashMap<String, String> meta = new LinkedHashMap<>();
    meta.put("protocol", "MQTT");
    meta.put("version", "4");
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
        .setTransformation(transformation)
        .setDestinationTransformer(transformer);

    return mb.build();
  }

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) throws MalformedException {
    checkState(session);

    Publish publish = (Publish) mqttPacket;
    MQTTPacket response = null;
    if (publish.getQos().equals(QualityOfService.AT_LEAST_ONCE)) {
      response = new PubAck(publish.getPacketId());
    } else if (publish.getQos().equals(QualityOfService.EXACTLY_ONCE)) {
      response = new PubRec(publish.getPacketId());
    }


    String lookup = publish.getDestinationName();


    Map<String, String> map = ((MQTTProtocol) protocol).getTopicNameMapping();
    if(map != null){
      lookup = map.get(publish.getDestinationName());
      if(lookup == null){
        lookup = publish.getDestinationName();
      }
    }

    if (!lookup.startsWith("$")) {
      try {
        Destination destination = session.findDestination(lookup);
        if(destination != null) {
          processMessage(publish, protocol, session, response, destination);
        }
      } catch (IOException e) {
        logger.log(LogMessages.MQTT_PUBLISH_STORE_FAILED, e);
        try {
          endPoint.close();
        } catch (IOException ioException) {
          // Ignore we are in an error state
        }
        throw new MalformedException("[MQTT-3.3.5-2]");
      }
    }
    return response;
  }

  private void processMessage(Publish publish, ProtocolImpl protocol, Session session, MQTTPacket response, Destination destination) throws IOException {
    Transformer transformer = protocol.destinationTransformationLookup(destination.getName() );
    Message message = createMessage(publish.getPayload(), publish.getPriority(), publish.isRetain(), publish.getQos(), protocol.getTransformation(), transformer);
    if(response != null){
      Transaction transaction = null;
      try {
        transaction = session.startTransaction(session.getName()+"_"+publish.getPacketId());
      } catch (TransactionException e) {
        logger.log(LogMessages.MQTT_DUPLICATE_EVENT_RECEIVED, publish.getPacketId());
      }
      if(transaction != null) {
        transaction.add(destination, message);
        if(publish.getQos().equals(QualityOfService.AT_LEAST_ONCE)){
          transaction.commit();
        }
      }
    }
    else {
      destination.storeMessage(message);
    }
  }
}
