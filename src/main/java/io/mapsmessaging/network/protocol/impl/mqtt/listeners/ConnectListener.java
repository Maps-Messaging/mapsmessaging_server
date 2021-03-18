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

package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.LogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.ConnAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ConnectListener extends BaseConnectionListener {

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
      SessionContextBuilder scb = getBuilder(endPoint, protocol, connect.getSessionId(), connect.isCleanSession(), connect.getKeepAlive(), connect.getUsername(), connect.getPassword());
      if (connect.isWillFlag()) {
        Message message = PublishListener.createMessage(connect.getWillMsg(), Priority.NORMAL, connect.isWillRetain(), connect.getWillQOS(), protocol.getTransformation(), null);
        scb.setWillMessage(message).setWillTopic(connect.getWillTopic());
      }
      protocol.setKeepAlive(connect.getKeepAlive());
      try {
        Session session = createSession(endPoint, protocol, scb, connect.getSessionId());
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
}
