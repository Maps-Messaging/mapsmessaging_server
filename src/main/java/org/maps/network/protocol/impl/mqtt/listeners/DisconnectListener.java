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

package org.maps.network.protocol.impl.mqtt.listeners;

import java.io.IOException;
import org.maps.logging.LogMessages;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionManager;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;

public class DisconnectListener extends PacketListener {

  @Override
  public MQTTPacket handlePacket(
      MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol)
      throws MalformedException {
    logger.log(LogMessages.MQTT_DISCONNECT_CLOSE);
    if (session != null) {
      try {
        SessionManager.getInstance().close(session, true);
      } catch (IOException e) {
        logger.log(LogMessages.SESSION_CLOSE_EXCEPTION, e);
      }
    }
    try {
      endPoint.close();
    } catch (IOException e) {
      logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
    }
    return null;
  }
}
