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
import io.mapsmessaging.config.auth.AuthConfig;
import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.AuthenticationContext;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.ConnAck5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.*;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class ConnAckListener5 extends PacketListener5 {

  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, Protocol protocol) throws MalformedException {
    AuthConfig config =  ((EndPointConnectionServerConfig)endPoint.getConfig()).getAuthConfig();

    String sess = config.getSessionId();
    String user = config.getUsername();
    String pass = config.getPassword();

    ConnAck5 connAck = (ConnAck5) mqttPacket;

    SessionContextBuilder scb = new SessionContextBuilder(sess, new ProtocolClientConnection(protocol));
    scb.setReceiveMaximum(((MQTT5Protocol) protocol).getClientReceiveMaximum());
    handleSessionCreation(protocol, sess, connAck, scb, endPoint, user, pass );
    protocol.setConnected(true);
    return null;
  }

  private void handleSessionCreation(Protocol protocol, String sessionId, ConnAck5 connAck, SessionContextBuilder scb, EndPoint endPoint, String user, String pass)
      throws MalformedException {
    Session session = null;
    try {
      session = createSession((MQTT5Protocol) protocol, connAck, scb, user, pass);
      ((MQTT5Protocol) protocol).setSession(session);
      ProtocolMessageTransformation transformation = TransformationManager.getInstance().getTransformation(
          endPoint.getProtocol(),
          endPoint.getName(),
          "mqtt",
          session.getSecurityContext().getUsername()
      );
      protocol.setProtocolMessageTransformation(transformation);
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
    connAck.setCallback(session::resumeState);
  }

  private Session createSession(MQTT5Protocol protocol, ConnAck5 connect, SessionContextBuilder scb, String user, String pass) throws LoginException, IOException {
    MessageProperty keepAliveProp = connect.getProperties().get(MessagePropertyFactory.SERVER_KEEPALIVE);
    int keepAlive = 60;
    if(keepAliveProp != null) {
      keepAlive = ((ServerKeepAlive) keepAliveProp).getServerKeepAlive();
    }
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
    scb.setPersistentSession(false);
    scb.setResetState(true);

    if (pass != null && !pass.isEmpty()) {
      scb.setPassword(pass.toCharArray());
    }
    if (user != null && !user.isEmpty()) {
      scb.setUsername(user);
    }

    String duplicateReport = connect.getProperties().getDuplicateReport();
    if (!duplicateReport.isEmpty()) {
      logger.log(ServerLogMessages.MQTT5_DUPLICATE_PROPERTIES_DETECTED, duplicateReport);
    }
    AuthenticationContext context = protocol.getAuthenticationContext();
    scb.isAuthorized(context != null && context.isComplete() && context.getUsername() != null);
    return SessionManager.getInstance().create(scb.build(), protocol);
  }
}