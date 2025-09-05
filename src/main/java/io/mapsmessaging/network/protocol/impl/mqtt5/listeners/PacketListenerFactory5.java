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

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;

public class PacketListenerFactory5 {

  protected final PacketListener5[] listeners;

  public PacketListenerFactory5() {
    listeners = new PacketListener5[16];
    for (int x = 0; x < listeners.length; x++) {
      listeners[x] = new NoOpListener();
    }
    listeners[MQTTPacket.CONNECT] = new ConnectListener5();
    listeners[MQTTPacket.DISCONNECT] = new DisconnectListener5();

    listeners[MQTTPacket.PUBLISH] = new PublishListener5();
    listeners[MQTTPacket.PINGREQ] = new PingRequestListener5();
    listeners[MQTTPacket.SUBSCRIBE] = new SubscribeListener5();
    listeners[MQTTPacket.PUBACK] = new PubAckListener5();
    listeners[MQTTPacket.PUBREL] = new PubRelListener5();
    listeners[MQTTPacket.PUBREC] = new PubRecListener5();
    listeners[MQTTPacket.PUBCOMP] = new PubCompListener5();
    listeners[MQTTPacket.UNSUBSCRIBE] = new UnsubscribeListener5();
    listeners[MQTTPacket5.AUTH] = new AuthListener5();
  }

  public PacketListener5 getListener(int mqttPacketId) {
    return listeners[mqttPacketId];
  }

  private static class NoOpListener extends PacketListener5 {

    @Override
    public MQTTPacket5 handlePacket(
        MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, Protocol protocol)
        throws MalformedException {
      return null;
    }
  }
}
