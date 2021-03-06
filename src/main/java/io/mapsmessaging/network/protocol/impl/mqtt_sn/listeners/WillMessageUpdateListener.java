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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.listeners;

import static io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket.NOT_SUPPORTED;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.WillTask;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.WillMessageResponse;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.WillMessageUpdate;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.state.StateEngine;

public class WillMessageUpdateListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine) {
    WillMessageUpdate willMessageUpdate = (WillMessageUpdate) mqttPacket;
    WillTask task = session.getWillTask();
    int status = 0;
    if (task != null) {
      byte[] payload = willMessageUpdate.getMessage();
      task.updateMessage(payload);
    } else {
      status = NOT_SUPPORTED;
    }
    return new WillMessageResponse(status);
  }
}