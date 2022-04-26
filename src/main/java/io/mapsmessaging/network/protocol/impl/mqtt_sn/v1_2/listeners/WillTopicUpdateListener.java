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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.WillTask;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillTopicResponse;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillTopicUpdate;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;

public class WillTopicUpdateListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine) {
    WillTopicUpdate willTopicUpdate = (WillTopicUpdate) mqttPacket;

    ReasonCodes status = ReasonCodes.Success;
    WillTask task = session.getWillTask();
    if(task != null){
      if (willTopicUpdate.getTopic() == null) {
        task.cancel();
      }
      else{
        task.updateQoS(willTopicUpdate.getQoS());
        task.updateTopic(willTopicUpdate.getTopic());
      }
    }
    else{
      status = ReasonCodes.NotSupported;
    }
    return new WillTopicResponse(status);
  }
}