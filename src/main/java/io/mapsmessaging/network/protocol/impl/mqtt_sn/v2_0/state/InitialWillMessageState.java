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

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillMessage;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.State;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.ConnAck;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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
  public String getName() {
    return "WillMessage";
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session oldSession, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) {
    if (mqtt.getControlPacketId() == MQTT_SNPacket.WILLMSG && oldSession == null) {
      WillMessage willMessage = (WillMessage) mqtt;
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(willMessage.getMessage())
          .setTransformation(protocol.getProtocolMessageTransformation())
          .setQoS(qualityOfService)
          .setRetain(retain);
      SessionContextBuilder scb = stateEngine.getSessionContextBuilder();
      Message message = MessageOverrides.createMessageBuilder(protocol.getProtocolConfig().getMessageDefaults(), messageBuilder).build();
      scb.setWillMessage(message);
      CompletableFuture<Session> sessionFuture = stateEngine.createSession(scb, protocol);
      sessionFuture.thenApply(session -> {
        protocol.setSession(session);
        ConnAck response = new ConnAck(ReasonCodes.SUCCESS, scb.getSessionExpiry(), scb.getId(), session.isRestored());
        response.setCallback(session::resumeState);
        ProtocolMessageTransformation transformation = TransformationManager.getInstance().getTransformation(
            endPoint.getProtocol(),
            endPoint.getName(),
            "mqtt-sn",
            session.getSecurityContext().getUsername()

        );
        protocol.setProtocolMessageTransformation(transformation);
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

  private void sendErrorResponse(MQTT_SNProtocol protocol) {
    ConnAck response = new ConnAck(ReasonCodes.NOT_SUPPORTED, 0, "", false);
    response.setCallback(() -> {
      try {
        protocol.close();
      } catch (IOException ioException) {
        // we can ignore this, we are about to close it since we have no idea what it is
      }
    });
  }

}
