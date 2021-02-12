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

package org.maps.network.protocol.impl.mqtt_sn.listeners;

import static org.maps.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket.NOT_SUPPORTED;

import org.maps.messaging.api.Session;
import org.maps.messaging.api.WillTask;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import org.maps.network.protocol.impl.mqtt_sn.packet.WillTopicResponse;
import org.maps.network.protocol.impl.mqtt_sn.packet.WillTopicUpdate;
import org.maps.network.protocol.impl.mqtt_sn.state.StateEngine;

public class WillTopicUpdateListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine) {
    WillTopicUpdate willTopicUpdate = (WillTopicUpdate) mqttPacket;

    int status = 0;
    if (willTopicUpdate.getTopic() == null) {
      session.getWillTask().cancel();
    } else {
      WillTask task = session.getWillTask();
      if (task != null) {
        task.updateQoS(willTopicUpdate.getQoS());
        task.updateTopic(willTopicUpdate.getTopic());
      } else {
        status = NOT_SUPPORTED;
      }
    }
    return new WillTopicResponse(status);
  }
}