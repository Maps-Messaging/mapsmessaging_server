/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PubComp;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PubRel;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;

import java.io.IOException;

public class PubRelListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(
      MQTT_SNPacket mqttPacket,
      Session session,
      EndPoint endPoint,
      ProtocolImpl protocol,
      StateEngine stateEngine) {

    PubRel pubRel = (PubRel) mqttPacket;

    PubComp event = new PubComp(pubRel.getMessageId());
    event.setCallback(() -> {
      Transaction tx = session.getTransaction(session.getName() + ":" + event.getMessageId());
      if (tx != null) {
        try {
          tx.commit();
          session.closeTransaction(tx);
        } catch (IOException e) {
          // catch & ignore
        }
      }
    });
    return event;
  }
}