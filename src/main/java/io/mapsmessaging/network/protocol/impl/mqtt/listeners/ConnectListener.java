/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.ConnAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ConnectListener extends BaseConnectionListener {

  private static final String RESTRICTED_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final int RESTRICTED_LENGTH = 23;

  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session shouldBeNull, EndPoint endPoint, ProtocolImpl protocol) throws MalformedException {
    if (shouldBeNull != null) {
      logger.log(ServerLogMessages.MQTT_CONNECT_LISTENER_SECOND_CONNECT);
      try {
        protocol.close();
      } catch (IOException e) {
        logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
      throw new MalformedException("[MQTT-3.1.0-2] Received a second CONNECT packet");
    }
    Connect connect = (Connect) mqttPacket;
    ConnAck connAck = new ConnAck();

    boolean strict = checkStrict(connect, protocol);

    String sessionId = connect.getSessionId();
    if ((!connect.isCleanSession() && sessionId.isEmpty()) || !clientIdAllowed(sessionId, strict)) {
      connAck.setResponseCode(ConnAck.IDENTIFIER_REJECTED);
    } else {
      createSession(sessionId, connect, connAck, endPoint, (MQTTProtocol) protocol);
      return null; // Delayed response
    }
    return connAck;
  }

  @Override
  public boolean resumeRead() {
    return false;
  }

  void createSession(String sessionId, Connect connect, ConnAck connAck, EndPoint endPoint, MQTTProtocol protocol) {
    CompletableFuture<Session> sessionFuture = constructSession(endPoint, protocol, sessionId, connect);
    sessionFuture.whenComplete((session, throwable) -> {
      if (throwable != null) {
        handleSessionException(throwable, connAck, protocol);
      } else {
        try {
          setupConnAck(session, connAck);
          protocol.registerRead();
          protocol.writeFrame(connAck);
        } catch (IOException e) {
          handleSessionException(e, connAck, protocol);
        }
      }
    });
  }


  private void setupConnAck(Session session, ConnAck connAck) {
    connAck.setResponseCode(ConnAck.SUCCESS);
    connAck.setRestoredFlag(session.isRestored());
    connAck.setCallback(session::resumeState);
  }

  private CompletableFuture<Session> constructSession(EndPoint endPoint, ProtocolImpl protocol, String sessionId, Connect connect) {
    SessionContextBuilder scb = getBuilder(endPoint, protocol, sessionId, connect.isCleanSession(), connect.getKeepAlive(), connect.getUsername(), connect.getPassword());
    if (connect.isWillFlag()) {
      Message message = PublishListener.createMessage(connect.getWillMsg(), Priority.NORMAL, connect.isWillRetain(), connect.getWillQOS(), protocol.getTransformation(), null);
      scb.setWillMessage(message).setWillTopic(connect.getWillTopic());
    }
    protocol.setKeepAlive(connect.getKeepAlive());
    return createSession(endPoint, protocol, scb, sessionId);
  }

  private void handleSessionException(Throwable e, ConnAck connAck, ProtocolImpl protocol) {
    connAck.setResponseCode(ConnAck.BAD_USERNAME_PASSWORD);
    connAck.setCallback(() -> SimpleTaskScheduler.getInstance().schedule(() -> {
      try {
        protocol.close();
      } catch (IOException e1) {
        logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e1);
      }
    }, 100, TimeUnit.MILLISECONDS));
  }

  boolean checkStrict(Connect connect, ProtocolImpl protocol) {
    boolean strict;
    if (connect.getProtocolLevel() == 3) {
      strict = true; // For MQTT 3.1 it must be strict to adhere to the standard
    } else {
      strict =((MQTTProtocol)protocol).getMqttConfig().isStrictClientId();
    }
    return strict;
  }

  boolean clientIdAllowed(String clientId, boolean strict) {
    if (strict) {
      boolean legalChars = clientId.chars().allMatch(c -> RESTRICTED_CHARACTERS.contains(String.valueOf((char) c)));
      return clientId.length() <= RESTRICTED_LENGTH && legalChars;
    } else {
      return true;
    }
  }

}
