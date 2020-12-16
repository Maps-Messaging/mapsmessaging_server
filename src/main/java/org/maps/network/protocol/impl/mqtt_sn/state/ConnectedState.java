/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.mqtt_sn.state;

import java.io.IOException;
import org.maps.messaging.api.Session;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;
import org.maps.network.protocol.impl.mqtt_sn.MQTT_SNProtocol;
import org.maps.network.protocol.impl.mqtt_sn.listeners.PacketListener;
import org.maps.network.protocol.impl.mqtt_sn.listeners.PacketListenerFactory;
import org.maps.network.protocol.impl.mqtt_sn.packet.Disconnect;
import org.maps.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import org.maps.network.protocol.impl.mqtt_sn.packet.PingResponse;
import org.maps.network.protocol.impl.mqtt_sn.packet.Publish;

/**
 * This MQTT-SN state represents a fully connected and authorised state, so all requests a client can make are handled here
 */
public class ConnectedState implements State {

  private final PacketListenerFactory packetListenerFactory;
  private final MQTT_SNPacket lastResponse;

  public ConnectedState(MQTT_SNPacket response) {
    packetListenerFactory = new PacketListenerFactory();
    lastResponse = response;
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session session, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) throws MalformedException {

    switch (mqtt.getControlPacketId()) {
      case MQTT_SNPacket.DISCONNECT:
        Disconnect disconnect = (Disconnect) mqtt;
        if (disconnect.getDuration() > 0) {
          stateEngine.setState(new SleepState(disconnect.getDuration(), protocol));
        } else {
          mqtt.setCallback(() -> {
            try {
              protocol.close();
            } catch (IOException e) {
              // Ignore this, we are closing
            }
          });
          stateEngine.setState(new DisconnectedState(mqtt));
        }
        return mqtt;

      case MQTT_SNPacket.CONNACK:
        break;

      case MQTT_SNPacket.PINGREQ:
        return new PingResponse();

      // Rebroadcast since these SHOULD ONLY be sent once and receiving them again implies lost
      // packet
      case MQTT_SNPacket.CONNECT:
      case MQTT_SNPacket.WILLMSG:
        return lastResponse;

      default:
        PacketListener listener = packetListenerFactory.getListener(mqtt.getControlPacketId());
        return listener.handlePacket(mqtt, session, endPoint, protocol, stateEngine);
    }
    return null;
  }

  @Override
  public void sendPublish(MQTT_SNProtocol protocol, String destination, Publish publish) {
    protocol.writeFrame(publish);
  }
}
