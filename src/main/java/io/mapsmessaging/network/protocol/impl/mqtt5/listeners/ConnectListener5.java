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

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.AuthenticationContext;
import io.mapsmessaging.network.protocol.impl.mqtt5.DefaultConstants;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.ConnAck5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Connect5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.*;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.security.uuid.UuidGenerator;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class ConnectListener5 extends PacketListener5 {

  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 mqttPacket, Session shouldBeNull, EndPoint endPoint, Protocol protocol) throws MalformedException {
    if (shouldBeNull != null) {
      logger.log(ServerLogMessages.MQTT5_SECOND_CONNECT);
      try {
        protocol.close();
      } catch (IOException e) {
        // Ignore this size we know its an issue and the end point may already be closed
      }
      throw new MalformedException("[MQTT-3.1.0-2]");
    }
    Connect5 connect = (Connect5) mqttPacket;
    ConnAck5 connAck = new ConnAck5();

    String sessionId = connect.getSessionId();
    if (sessionId.isEmpty()) {
      sessionId = UuidGenerator.getInstance().generate().toString();
      connAck.add(new AssignedClientIdentifier(sessionId));
    }

    SessionContextBuilder scb = new SessionContextBuilder(sessionId, new ProtocolClientConnection(protocol));
    scb.setReceiveMaximum(((MQTT5Protocol) protocol).getClientReceiveMaximum());
    boolean sendResponseInfo = parseProperties((MQTT5Protocol) protocol, connect, scb);

    if (!connect.isCleanSession() && sessionId.isEmpty()) {
      connAck.setStatusCode(StatusCode.CLIENT_IDENTIFIER_NOT_VALID);
    } else {
      handleSessionCreation(protocol, sessionId, connect, connAck, scb, endPoint);
    }

    if (connAck.getStatusCode() != StatusCode.SUCCESS) {
      ((MQTT5Protocol) protocol).setClosing(true);
      connAck.setCallback(() -> {
        try {
          endPoint.close();
        } catch (IOException e) {
          logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
        }
      });
      logger.log(ServerLogMessages.MQTT5_CONNECTION_FAILED, connAck.toString());
    }
    if (sendResponseInfo) {
      connAck.add(new ResponseInformation(connAck.getStatusCode().getDescription()));
    }
    return connAck;
  }

  private void handleSessionCreation(Protocol protocol, String sessionId, Connect5 connect, ConnAck5 connAck, SessionContextBuilder scb, EndPoint endPoint)
      throws MalformedException {
    Session session = null;
    try {
      session = createSession((MQTT5Protocol) protocol, sessionId, connect, scb);
      ((MQTT5Protocol) protocol).setSession(session);
      ProtocolMessageTransformation transformation = TransformationManager.getInstance().getTransformation(
          endPoint.getProtocol(),
          endPoint.getName(),
          "mqtt",
          session.getSecurityContext().getUsername()
      );
      protocol.setTransformation(transformation);
    } catch (LoginException e) {
      logger.log(ServerLogMessages.MQTT5_FAILED_CONSTRUCTION, e, sessionId);
      try {
        endPoint.close();
      } catch (IOException ioException) {
        // Ignore this since we know its in error anyway
      }
      throw new MalformedException("The Server MUST process a second CONNECT packet sent from a Client as a Protocol Error and close the Network Connection [MQTT-3.1.0-2]");
    } catch (IOException ioe) {
      logger.log(ServerLogMessages.MQTT5_FAILED_CONSTRUCTION, ioe, sessionId);
      connAck.setStatusCode(StatusCode.TOPIC_NAME_INVALID);
    }

    try {
      if (session != null) {
        login(session, connAck, (MQTT5Protocol) protocol);
      }
    } catch (IOException e) {
      connAck.setStatusCode(StatusCode.BAD_USERNAME_PASSWORD);
    }
  }

  private void login(Session session, ConnAck5 connAck, MQTT5Protocol protocol) throws IOException {
    session.login();
    connAck.setStatusCode(StatusCode.SUCCESS);
    connAck.setRestoredFlag(session.isRestored());
    connAck.add(
            new ServerReference(
                "Build Date:"
                    + BuildInfo.getBuildDate()
                    + " BuildVersion:"
                    + BuildInfo.getBuildVersion()))
        .add(new RetainAvailable(true))
        .add(new SubscriptionIdentifiersAvailable(true))
        .add(new WildcardSubscriptionsAvailable(true))
        .add(new SharedSubscriptionsAvailable(true))
        .add(new ServerKeepAlive(((int) protocol.getTimeOut() / 1000)))
        .add(new ReceiveMaximum(protocol.getServerReceiveMaximum()))
        .add(new TopicAliasMaximum(protocol.getClientTopicAliasMapping().getMaximum()));
    if(protocol.getAuthenticationContext() != null && protocol.getAuthenticationContext().getAuthenticationMethod() != null){
      String auth = protocol.getAuthenticationContext().getAuthenticationMethod().getAuthenticationMethod();
      connAck.add(new AuthenticationMethod(auth));
    }
    connAck.setCallback(session::resumeState);
  }

  private Session createSession(MQTT5Protocol protocol, String sessionId, Connect5 connect, SessionContextBuilder scb) throws LoginException, IOException {
    int keepAlive = connect.getKeepAlive();
    int minKeepAlive = protocol.getMinimumKeepAlive();
    int maxKeepAlive = (int) protocol.getTimeOut() / 1000;

    if (keepAlive == 0 && minKeepAlive != 0) { // Special case
      keepAlive = maxKeepAlive; // Push to the end of acceptable range
    } else if (keepAlive < minKeepAlive) {
      keepAlive = minKeepAlive; // Move to the lowest we accept
    } else if (keepAlive > maxKeepAlive) {
      keepAlive = maxKeepAlive; // Pull back to the maximum delay we accept
    }
    protocol.setKeepAlive(keepAlive * 1000L);

    scb.setResetState(connect.isCleanSession());

    if (connect.hasPassword()) {
      scb.setPassword(connect.getPassword());
    }
    if (connect.hasUsername()) {
      scb.setUsername(connect.getUsername());
    }

    if (connect.isWillFlag()) {
      Message message =
          PublishListener5.createMessage(
              sessionId,
              connect.getWillProperties().values(),
              DefaultConstants.PRIORITY,
              connect.isWillRetain(),
              connect.getWillMsg(),
              connect.getWillQOS(),
              protocol.getTransformation(),
              protocol);
      scb.setWillMessage(message).setWillTopic(connect.getWillTopic());
    }

    for (MessageProperty property : connect.getWillProperties().values()) {
      if (property.getId() == MessagePropertyFactory.WILL_DELAY_INTERVAL) {
        scb.setWillDelayInterval(((WillDelayInterval) property).getWillDelayInterval());
      }
    }
    String duplicateReport = connect.getProperties().getDuplicateReport();
    if (!duplicateReport.isEmpty()) {
      logger.log(ServerLogMessages.MQTT5_DUPLICATE_PROPERTIES_DETECTED, duplicateReport);
    }
    AuthenticationContext context = protocol.getAuthenticationContext();
    if(context != null && context.isComplete() && context.getUsername() != null){
      scb.setUsername(context.getUsername());
      scb.isAuthorized(true);
    }
    else{
      scb.isAuthorized(false);
    }
    return SessionManager.getInstance().create(scb.build(), protocol);
  }

  private boolean parseProperties(MQTT5Protocol protocol, Connect5 connect, SessionContextBuilder scb) {
    boolean sendResponseInfo = false;
    scb.setSessionExpiry(DefaultConstants.SESSION_TIME_OUT);
    scb.setReceiveMaximum(DefaultConstants.CLIENT_RECEIVE_MAXIMUM);
    scb.setPersistentSession(DefaultConstants.SESSION_TIME_OUT != 0);
    for (MessageProperty property : connect.getProperties().values()) {
      switch (property.getId()) {
        case MessagePropertyFactory.SESSION_EXPIRY_INTERVAL:
          scb.setSessionExpiry(((SessionExpiryInterval) property).getExpiry());
          scb.setPersistentSession(scb.getSessionExpiry() != 0);
          break;

        case MessagePropertyFactory.RECEIVE_MAXIMUM:
          scb.setReceiveMaximum(((ReceiveMaximum) property).getReceiveMaximum());
          break;

        case MessagePropertyFactory.MAXIMUM_PACKET_SIZE:
          protocol.setMaxBufferSize(((MaximumPacketSize) property).getMaximumPacketSize());
          break;

        case MessagePropertyFactory.TOPIC_ALIAS_MAXIMUM:
          protocol.getServerTopicAliasMapping().setMaximum(((TopicAliasMaximum) property).getTopicAliasMaximum());
          break;

        case MessagePropertyFactory.REQUEST_RESPONSE_INFORMATION:
          sendResponseInfo = true;
          break;

        case MessagePropertyFactory.REQUEST_PROBLEM_INFORMATION:
          protocol.setSendProblemInformation(true);
          break;

        case MessagePropertyFactory.USER_PROPERTY:
          scb.setUserProperty((UserProperty) property);
          break;

        default:
          logger.log(ServerLogMessages.MQTT5_UNHANDLED_PROPERTY, property.toString());
      }
    }
    return sendResponseInfo;
  }
}
