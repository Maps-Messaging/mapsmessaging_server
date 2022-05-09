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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.state;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillMessage;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.State;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.ConnAck;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
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
      CompletableFuture<Session> sessionFuture = stateEngine.createSession(scb, protocol);
      sessionFuture.thenApply(session -> {
        protocol.setSession(session);
        ConnAck response = new ConnAck(ReasonCodes.Success, scb.getSessionExpiry(), scb.getId() );
        response.setCallback(session::resumeState);
        protocol.setTransformation(TransformationManager.getInstance().getTransformation(protocol.getName(), session.getSecurityContext().getUsername()));
        try {
          session.login();
          stateEngine.setState(new ConnectedState(response));
        } catch (IOException e) {
          sendErrorResponse(protocol);
          return null;
        }
        protocol.writeFrame(response);
        return session;
      }).exceptionally(exception -> {
        sendErrorResponse(protocol);
        return null;
      });
    } else if (mqtt.getControlPacketId() == MQTT_SNPacket.WILLTOPIC) { // Retransmit received
      return lastResponse;
    }

    return null;
  }

  private void sendErrorResponse(MQTT_SNProtocol protocol){
    ConnAck response = new ConnAck(ReasonCodes.NotSupported, 0, "");
    response.setCallback(() -> {
      try {
        protocol.close();
      } catch (IOException ioException) {
        // we can ignore this, we are about to close it since we have no idea what it is
      }
    });
  }

}
