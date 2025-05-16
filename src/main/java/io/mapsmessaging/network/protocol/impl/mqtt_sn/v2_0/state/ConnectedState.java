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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListenerFactory;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.State;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.listeners.PacketListenerFactoryV2;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Disconnect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.PingResponse;

import java.io.IOException;

/**
 * This MQTT-SN state represents a fully connected and authorised state, so all requests a client can make are handled here
 */
public class ConnectedState implements State {

  private final PacketListenerFactory packetListenerFactory;
  private final MQTT_SNPacket lastResponse;

  public ConnectedState(MQTT_SNPacket response) {
    packetListenerFactory = new PacketListenerFactoryV2();
    lastResponse = response;
  }

  @Override
  public String getName() {
    return "Connected";
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session session, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) throws MalformedException {

    switch (mqtt.getControlPacketId()) {
      case MQTT_SNPacket.DISCONNECT:
        Disconnect disconnect = (Disconnect) mqtt;
        String reason = "Disconnected";
        if (disconnect.getExpiry() > 0) {
          reason = "Sleeping";
        }
        MQTT_SNPacket response = new Disconnect(ReasonCodes.SUCCESS, disconnect.getExpiry(), reason);
        if (disconnect.getExpiry() > 0) {
          stateEngine.setState(new SleepState((int) disconnect.getExpiry(), protocol));
          stateEngine.sleep();
        } else {
          response.setCallback(() -> {
            try {
              SessionManager.getInstance().close(session, true);
              protocol.close();
            } catch (IOException e) {
              // Ignore this, we are closing
            }
          });
          stateEngine.setState(new DisconnectedState(response));
        }
        return response;

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
  public void sendPublish(MQTT_SNProtocol protocol, String destination, MQTT_SNPacket publish) {
    protocol.writeFrame(publish);
  }
}
