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

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Disconnect5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageProperty;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.ReasonString;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SessionExpiryInterval;
import java.io.IOException;

public class DisconnectListener5 extends PacketListener5 {

  @Override
  public MQTTPacket5 handlePacket(
      MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) {
    Disconnect5 disconnect = (Disconnect5) mqttPacket;
    for (MessageProperty property : disconnect.getProperties().values()) {
      switch (property.getId()) {
        case MessagePropertyFactory.SESSION_EXPIRY_INTERVAL:
          session.setExpiryTime(((SessionExpiryInterval) property).getExpiry());
          break;

        case MessagePropertyFactory.REASON_STRING:
          logger.log(
              ServerLogMessages.MQTT5_DISCONNECT_REASON, ((ReasonString) property).getReasonString());
          break;

        default:
          break;
      }
    }
    if (session != null) {
      logger.log(ServerLogMessages.MQTT5_DISCONNECTING_SESSION, session.getName());
      try {
        SessionManager.getInstance().close(session, disconnect.getDisconnectReason().equals(StatusCode.SUCCESS));
      } catch (IOException e) {
        logger.log(ServerLogMessages.SESSION_CLOSE_EXCEPTION, e);
      }
    }
    try {
      protocol.close();
      endPoint.close();
    } catch (IOException e) {
      logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
    }
    return null;
  }
}
