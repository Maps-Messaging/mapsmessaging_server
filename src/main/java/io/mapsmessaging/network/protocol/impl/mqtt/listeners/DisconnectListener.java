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
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import java.io.IOException;

public class DisconnectListener extends PacketListener {

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) throws MalformedException {
    logger.log(ServerLogMessages.MQTT_DISCONNECT_CLOSE);
    if (session != null) {
      try {
        SessionManager.getInstance().close(session, true);
      } catch (IOException e) {
        logger.log(ServerLogMessages.SESSION_CLOSE_EXCEPTION, e);
      }
    }
    try {
      protocol.close();
    } catch (IOException e) {
      logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
    }
    return null;
  }
}
