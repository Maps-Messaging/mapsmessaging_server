/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.mqtt.listeners;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;
import org.maps.logging.LogMessages;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionContextBuilder;
import org.maps.messaging.api.SessionManager;
import org.maps.messaging.api.features.Priority;
import org.maps.messaging.api.message.Message;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.DefaultConstants;
import org.maps.network.protocol.impl.mqtt.MQTTProtocol;
import org.maps.network.protocol.impl.mqtt.packet.ConnAck;
import org.maps.network.protocol.impl.mqtt.packet.Connect;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;
import org.maps.network.protocol.transformation.TransformationManager;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class ConnectListener extends PacketListener {

  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session shouldBeNull, EndPoint endPoint, ProtocolImpl protocol) throws MalformedException {
    if (shouldBeNull != null) {
      logger.log(LogMessages.MQTT_CONNECT_LISTENER_SECOND_CONNECT);
      try {
        protocol.close();
      } catch (IOException e) {
        logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
      throw new MalformedException("[MQTT-3.1.0-2] Received a second CONNECT packet");
    }
    Connect connect = (Connect) mqttPacket;
    ConnAck connAck = new ConnAck();

    if (!connect.isCleanSession() && connect.getSessionId().length() == 0) {
      connAck.setResponseCode(ConnAck.IDENTIFIER_REJECTED);
    } else {
      SessionContextBuilder scb = getBuilder(endPoint, protocol, connect);
      protocol.setKeepAlive(connect.getKeepAlive());
      try {
        Session session = createSession(endPoint, protocol, scb, connect);
        connAck.setResponseCode(ConnAck.SUCCESS);
        connAck.setRestoredFlag(session.isRestored());
        connAck.setCallback(session::resumeState);
      } catch (IOException e) {
        logger.log(LogMessages.MQTT_BAD_USERNAME_PASSWORD, e);
        connAck.setResponseCode(ConnAck.BAD_USERNAME_PASSWORD);
      }
    }

    if (connAck.getResponseCode() != ConnAck.SUCCESS) {
      connAck.setCallback(() -> SimpleTaskScheduler.getInstance().schedule(() -> {
        try {
          protocol.close();
        } catch (IOException e) {
          logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
        }
      }, 100, TimeUnit.MILLISECONDS));
    }
    return connAck;
  }

  private Session createSession(EndPoint endPoint, ProtocolImpl protocol, SessionContextBuilder scb, Connect connect) throws MalformedException, IOException {
    Session session;
    try {
      session = SessionManager.getInstance().create(scb.build(), protocol);
      ((MQTTProtocol) protocol).setSession(session);
    } catch (LoginException e) {
      logger.log(LogMessages.MQTT_CONNECT_LISTENER_SESSION_EXCEPTION, e, connect.getSessionId());
      endPoint.close();
      throw new MalformedException("[MQTT-3.1.0-2] Failed to create the session for the MQTT session");
    } catch (IOException ioe) {
      logger.log(LogMessages.MQTT_CONNECT_LISTENER_SESSION_EXCEPTION, ioe, connect.getSessionId());
      endPoint.close();
      throw new MalformedException("Unable to construct the required Will Topic");
    }
    session.login();
    protocol.setTransformation(TransformationManager.getInstance().getTransformation(protocol.getName(), session.getSecurityContext().getUsername()));
    return session;
  }

  private SessionContextBuilder getBuilder(EndPoint endPoint, ProtocolImpl protocol, Connect connect){
    SessionContextBuilder scb = new SessionContextBuilder(connect.getSessionId(), protocol);
    scb.setPersistentSession(true)
        .setResetState(connect.isCleanSession())
        .setKeepAlive(connect.getKeepAlive())
        .setSessionExpiry(endPoint.getConfig().getProperties().getIntProperty("maximumSessionExpiry", DefaultConstants.SESSION_TIME_OUT));

    if (connect.isPasswordFlag()) {
      scb.setPassword(connect.getPassword());
    }
    if (connect.isUsernameFlag()) {
      scb.setUsername(connect.getUsername());
    }

    if (connect.isWillFlag()) {
      Message message = PublishListener.createMessage(connect.getWillMsg(), Priority.NORMAL, connect.isWillRetain(), connect.getWillQOS(), protocol.getTransformation());
      scb.setWillMessage(message).setWillTopic(connect.getWillTopic());
    }
    scb.setReceiveMaximum(DefaultConstants.RECEIVE_MAXIMUM);
    return scb;
  }
}
