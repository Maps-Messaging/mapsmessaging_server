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
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;

public class PacketListenerFactory {

  protected final PacketListener[] listeners;

  public PacketListenerFactory() {
    listeners = new PacketListener[16];
    for (int x = 0; x < listeners.length; x++) {
      listeners[x] = new NoOpListener();
    }
    //
    // Server based events
    //
    listeners[MQTTPacket.CONNECT] = new ConnectListener();
    listeners[MQTTPacket.PUBLISH] = new PublishListener();
    listeners[MQTTPacket.PUBREL] = new PubRelListener();
    listeners[MQTTPacket.DISCONNECT] = new DisconnectListener();
    listeners[MQTTPacket.PINGREQ] = new PingRequestListener();
    listeners[MQTTPacket.SUBSCRIBE] = new SubscribeListener();
    listeners[MQTTPacket.PUBACK] = new PubAckListener();
    listeners[MQTTPacket.PUBREC] = new PubRecListener();
    listeners[MQTTPacket.PUBCOMP] = new PubCompListener();
    listeners[MQTTPacket.UNSUBSCRIBE] = new UnsubscribeListener();

    //
    // Client based events
    //
    listeners[MQTTPacket.CONNACK] = new ConnAckListener();
    listeners[MQTTPacket.SUBACK] = new SubAckListener();
    listeners[MQTTPacket.PINGRESP] = new PingResponseListener();
  }

  public PacketListener getListener(int mqttPacketId) {
    return listeners[mqttPacketId];
  }

  private static class NoOpListener extends PacketListener {

    @Override
    public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, Protocol protocol) throws MalformedException {
      return null;
    }
  }
}
