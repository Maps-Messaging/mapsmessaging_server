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
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.dto.rest.config.auth.SaslConfigDTO;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillTopicRequest;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.State;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MQTT_SNProtocolV2;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Auth;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.ConnAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MQTT_SN_2_Packet;
import io.mapsmessaging.network.protocol.sasl.SaslAuthenticationMechanism;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session oldSession, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) throws IOException {
    if (mqtt.getControlPacketId() == MQTT_SNPacket.CONNECT) {
      SaslAuthenticationMechanism saslAuthenticationMechanism = ((MQTT_SNProtocolV2)protocol).getSaslAuthenticationMechanism();
      SaslConfigDTO saslConfig = endPoint.getConfig().getSaslConfig();

      Connect connect = (Connect) mqtt;

      boolean serverRequiresAuth = saslConfig != null;
      boolean requiresAuth = connect.isAuthentication();
      if(serverRequiresAuth && ( !requiresAuth && saslAuthenticationMechanism == null)){
        sendErrorResponse(protocol, ReasonCodes.BAD_AUTH);
        return null;
      }

      stateEngine.setMaxBufferSize(connect.getMaxPacketSize());
      SessionContextBuilder scb = new SessionContextBuilder(connect.getClientId(), new ProtocolClientConnection(protocol));
      scb.setResetState(connect.isCleanStart());
      scb.setPersistentSession(!connect.isCleanStart());

      int receiveMax = ((MqttSnConfig)endPoint.getConfig().getProtocolConfig("mqtt-sn")).getReceiveMaximum();
      scb.setReceiveMaximum(receiveMax);
      scb.setSessionExpiry(connect.getSessionExpiry());
      if(saslAuthenticationMechanism != null){
        String username  = saslAuthenticationMechanism.getUsername();
        if(username != null) {
          scb.setUsername(username);
          scb.isAuthorized(true);
        }
      }
      protocol.setKeepAlive(TimeUnit.SECONDS.toMillis(connect.getKeepAlive()));
      if (connect.isWill()) {
        stateEngine.setSessionContextBuilder(scb);
        WillTopicRequest topicRequest = new WillTopicRequest();
        InitialWillTopicState nextState = new InitialWillTopicState(topicRequest);
        stateEngine.setState(nextState);
        return topicRequest;
      } else {
        if(requiresAuth){
          if(saslConfig != null){
            Map<String, String> authProps = new HashMap<>();
            authProps.put(Sasl.QOP, "auth");
            try {
              String serverName = saslConfig.getRealmName();
              saslAuthenticationMechanism = new SaslAuthenticationMechanism(saslConfig.getMechanism(), serverName, "mqtt-sn", authProps, endPoint.getConfig());
              stateEngine.setState(new AuthenticationState(connect, saslAuthenticationMechanism));
              ((MQTT_SNProtocolV2)protocol).setSaslAuthenticationMechanism(saslAuthenticationMechanism);
              return new Auth(ReasonCodes.CONTINUE_AUTHENTICATION, saslAuthenticationMechanism.getName(), new byte[0]);
            } catch (SaslException e) {
              // Close
              try {
                endPoint.close();
              } catch (IOException ex) {
                throw new RuntimeException(ex);
              }
            }
          }
          else{
            try {
              endPoint.close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
          return null;
        }
        CompletableFuture<Session> sessionFuture = stateEngine.createSession(scb, protocol);
        sessionFuture.thenApply(session -> {
          protocol.setSession(session);
          ConnAck response = new ConnAck(ReasonCodes.SUCCESS, 0, scb.getId(), session.isRestored());
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
            protocol.writeFrame(response);
            return session;
          } catch (IOException e) {
            sendErrorResponse(protocol, ReasonCodes.NOT_SUPPORTED);
            return null;
          }
        }).exceptionally(exception -> {
          sendErrorResponse(protocol, ReasonCodes.NOT_SUPPORTED);
          return null;
        });
      }
    }

    else if (mqtt.getControlPacketId() == MQTT_SN_2_Packet.AUTH) {
      Auth auth = (Auth) mqtt;
      try {
        SaslAuthenticationMechanism saslAuthenticationMechanism = ((MQTT_SNProtocolV2)protocol).getSaslAuthenticationMechanism();
        saslAuthenticationMechanism.challenge(auth.getData());
      } catch (IOException e) {
        e.printStackTrace();
        // Log this and allow the session to be closed
      }
    }
    return null;
  }



  private void sendErrorResponse(MQTT_SNProtocol protocol, ReasonCodes reasonCodes) {
    ConnAck response = new ConnAck(reasonCodes, 0, null, false);
    response.setCallback(() -> {
      try {
        protocol.close();
      } catch (IOException ioException) {
        // we can ignore this, we are about to close it since we have no idea what it is
      }
    });
  }
}
