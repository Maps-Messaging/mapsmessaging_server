/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;

public abstract class PacketListener {

  protected Logger logger = LoggerFactory.getLogger("MQTT_Packet_Listener");

  public boolean resumeRead() {
    return true;
  }

  void checkState(Session session) throws MalformedException {
    if(session == null){
      throw new MalformedException("Connection has not yet been established");
    }
  }

  public abstract MQTTPacket handlePacket(
      MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol)
      throws MalformedException;
}
