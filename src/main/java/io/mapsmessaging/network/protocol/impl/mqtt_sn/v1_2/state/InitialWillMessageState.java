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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ConnAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillMessage;
import java.io.IOException;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class InitialWillMessageState implements State {

  private final MQTT_SNPacket lastResponse;
  private final QualityOfService qualityOfService;
  private final boolean retain;

  public InitialWillMessageState(MQTT_SNPacket response, @NonNull @NotNull QualityOfService qualityOfService, boolean retain) {
    lastResponse = response;
    this.retain = retain;
    this.qualityOfService = qualityOfService;
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session oldSession, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) {
    if (mqtt.getControlPacketId() == MQTT_SNPacket.WILLMSG && oldSession == null) {
      WillMessage willMessage = (WillMessage) mqtt;
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(willMessage.getMessage())
          .setTransformation(protocol.getTransformation())
          .setQoS(qualityOfService)
          .setRetain(retain);
      SessionContextBuilder scb = stateEngine.getSessionContextBuilder();
      scb.setWillMessage(messageBuilder.build());
      try {
        MQTT_SNPacket response = new ConnAck(ReasonCodes.Success);
        stateEngine.createSession(scb, protocol, response);
        return response;
      } catch (LoginException | IOException e) {
        ConnAck response = new ConnAck(ReasonCodes.NotSupported);
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
