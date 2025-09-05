/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.WillTask;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.engine.session.will.WillDetails;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillTopicResponse;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillTopicUpdate;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;

public class WillTopicUpdateListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, Protocol protocol, StateEngine stateEngine) {
    WillTopicUpdate willTopicUpdate = (WillTopicUpdate) mqttPacket;
    ReasonCodes status = ReasonCodes.SUCCESS;
    WillTask task = session.getWillTask();
    if (task == null) {
      WillDetails willDetails = new WillDetails();
      willDetails.setSessionId(session.getName());
      willDetails.setDestination(willTopicUpdate.getTopic());
      willDetails.setVersion(protocol.getVersion());
      willDetails.setDelay(0);
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setQoS(willTopicUpdate.getQoS());
      messageBuilder.setRetain(willTopicUpdate.isRetain());
      willDetails.setMsg( MessageOverrides.createMessageBuilder(protocol.getProtocolConfig().getMessageDefaults(), messageBuilder).build());
      session.updateWillTopic(willDetails);
    } else {
      if (willTopicUpdate.getTopic() == null) {
        task.cancel();
      } else {
        task.updateQoS(willTopicUpdate.getQoS());
        task.updateRetainFlag(willTopicUpdate.isRetain());
        task.updateTopic(willTopicUpdate.getTopic());
      }
    }
    return new WillTopicResponse(status);
  }
}