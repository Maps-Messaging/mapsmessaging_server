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
import javax.security.auth.login.LoginException;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionContextBuilder;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.impl.mqtt_sn.MQTT_SNProtocol;
import org.maps.network.protocol.impl.mqtt_sn.packet.ConnAck;
import org.maps.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import org.maps.network.protocol.impl.mqtt_sn.packet.WillMessage;

public class InitialWillMessageState implements State {

  private final MQTT_SNPacket lastResponse;

  public InitialWillMessageState(MQTT_SNPacket response) {
    lastResponse = response;
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session oldSession, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) {
    if (mqtt.getControlPacketId() == MQTT_SNPacket.WILLMSG && oldSession == null) {
      WillMessage willMessage = (WillMessage) mqtt;
      willMessage.getMessage();
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(willMessage.getMessage())
          .setTransformation(protocol.getTransformation())
          .setQoS(willMessage.getQoS())
          .setRetain(willMessage.retain());
      SessionContextBuilder scb = stateEngine.getSessionContextBuilder();
      scb.setWillMessage(messageBuilder.build());
      try {
        MQTT_SNPacket response = new ConnAck(MQTT_SNPacket.ACCEPTED);
        stateEngine.createSession(scb, protocol, response);
        return response;
      } catch (LoginException | IOException e) {
        ConnAck response = new ConnAck(MQTT_SNPacket.NOT_SUPPORTED);
        response.setCallback(() -> {
          try {
            protocol.close();
          } catch (IOException ioException) {
            // we can ignore this since we are pulling the pin
          }
        });
        return response;
      }
    } else if (mqtt.getControlPacketId() == MQTT_SNPacket.WILLTOPIC) { // Retransmit received
      return lastResponse;
    }

    return null;
  }

}
