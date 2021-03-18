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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;

public class DisconnectedState implements State {

  private final MQTT_SNPacket lastResponse;

  public DisconnectedState(MQTT_SNPacket response) {
    lastResponse = response;
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session session, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) {
    if (mqtt.getControlPacketId() == MQTT_SNPacket.DISCONNECT) {
      return lastResponse;
    }
    return null;
  }

}
