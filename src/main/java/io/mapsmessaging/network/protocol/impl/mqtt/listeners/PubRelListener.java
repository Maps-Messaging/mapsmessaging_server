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

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PubComp;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PubRel;

import java.io.IOException;

public class PubRelListener extends PacketListener {

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, Protocol protocol) throws MalformedException {
    PubRel pubRel = (PubRel) mqttPacket;
    Transaction transaction = session.getTransaction(session.getName() + "_" + pubRel.getPacketIdentifier());
    if (transaction != null) {
      try {
        transaction.commit();
      } catch (IOException e) {
        try {
          protocol.close();
        } catch (IOException ioException) {
          // Closing the protocol
        }
      }
    }
    PubComp pubComp = new PubComp(pubRel.getPacketIdentifier());
    pubComp.setCallback(()-> {
      if (transaction != null) {
        try {
          session.closeTransaction(transaction);
        } catch (IOException e) {
          // catch & ignore
        }
      }
    });
    return pubComp;
  }
}
