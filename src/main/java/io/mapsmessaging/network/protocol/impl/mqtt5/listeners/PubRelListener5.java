/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubComp5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubRel5;
import java.io.IOException;

public class PubRelListener5 extends PacketListener5 {

  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) {
    PubRel5 pubRel = (PubRel5) mqttPacket;
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
    PubComp5 pubComp = new PubComp5(pubRel.getPacketIdentifier());
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
