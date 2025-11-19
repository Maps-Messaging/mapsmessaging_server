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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Protocol dictates that we need to a) Receive a Connect packet b) Respond with a Will Topic Request c) Receive a Will Topic Response d) Respond with a Will Message Request e)
 * Receive a Will Message response f) At this point we can establish a valid MQTT session and we respond with a ConAck response
 */
public class InitialConnectionState implements State {

  @Override
  public String getName() {
    return "Initial";
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session oldSession, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) {
    if (mqtt.getControlPacketId() == MQTT_SNPacket.CONNECT) {
      Connect connect = (Connect) mqtt;
      SessionContextBuilder scb = new SessionContextBuilder(connect.getClientId(), new ProtocolClientConnection(protocol));
      scb.setResetState(connect.clean());
      protocol.setKeepAlive(TimeUnit.SECONDS.toMillis(connect.getDuration()));
      MqttSnConfig config = (MqttSnConfig)endPoint.getConfig().getProtocolConfig("mqtt-sn");

      scb.setReceiveMaximum(config.getReceiveMaximum());
      scb.setSessionExpiry(config.getMaximumSessionExpiry());
      scb.setPersistentSession(!connect.clean());
      if (connect.will()) {
        stateEngine.setSessionContextBuilder(scb);
        WillTopicRequest topicRequest = new WillTopicRequest();
        InitialWillTopicState nextState = new InitialWillTopicState(topicRequest);
        stateEngine.setState(nextState);
        return topicRequest;
      } else {
        CompletableFuture<Session> sessionFuture = stateEngine.createSession(scb, protocol);
        sessionFuture.thenApply(session -> {
          protocol.setSession(session);
          ConnAck response = new ConnAck(ReasonCodes.SUCCESS);
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
      }
    }
    return null;
  }

  private void sendErrorResponse(MQTT_SNProtocol protocol) {
    ConnAck response = new ConnAck(ReasonCodes.NOT_SUPPORTED);
    response.setCallback(() -> {
      try {
        protocol.close();
      } catch (IOException ioException) {
        // we can ignore this, we are about to close it since we have no idea what it is
      }
    });
  }
}
