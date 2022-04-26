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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PubAckListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PubRelListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PublishListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.RegisterListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.SubscribeListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.UnsubscribeListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.WillMessageUpdateListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.WillTopicUpdateListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;

public class PacketListenerFactoryV2 {

  protected final PacketListener[] listeners;

  public PacketListenerFactoryV2() {
    listeners = new PacketListener[0xFF];
    for (int x = 0; x < listeners.length; x++) {
      listeners[x] = new NoOpListener();
    }
    listeners[MQTT_SNPacket.SUBSCRIBE] = new SubscribeListener();
    listeners[MQTT_SNPacket.UNSUBSCRIBE] = new UnsubscribeListener();
    listeners[MQTT_SNPacket.REGISTER] = new RegisterListener();
    listeners[MQTT_SNPacket.PUBLISH] = new PublishListener();
    listeners[MQTT_SNPacket.PUBREL] = new PubRelListener();
    listeners[MQTT_SNPacket.PUBACK] = new PubAckListener();
    listeners[MQTT_SNPacket.WILLMSGUPD] = new WillMessageUpdateListener();
    listeners[MQTT_SNPacket.WILLTOPICUPD] = new WillTopicUpdateListener();
  }

  public PacketListener getListener(int mqttPacketId) {
    return listeners[mqttPacketId];
  }

  private static class NoOpListener extends PacketListener {

    @Override
    public MQTT_SNPacket handlePacket(
        MQTT_SNPacket mqttPacket,
        Session session,
        EndPoint endPoint,
        ProtocolImpl protocol,
        StateEngine stateEngine) {
      return null;
    }
  }
}
