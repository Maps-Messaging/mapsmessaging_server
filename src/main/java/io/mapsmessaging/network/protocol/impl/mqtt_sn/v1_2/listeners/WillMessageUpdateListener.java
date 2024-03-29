/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.WillTask;
import io.mapsmessaging.engine.session.will.WillDetails;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillMessageResponse;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillMessageUpdate;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;

public class WillMessageUpdateListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine) {
    WillMessageUpdate willMessageUpdate = (WillMessageUpdate) mqttPacket;
    WillTask task = session.getWillTask();
    byte[] payload = willMessageUpdate.getMessage();
    ReasonCodes status = ReasonCodes.SUCCESS;
    if (task == null) {
      WillDetails willDetails = new WillDetails();
      willDetails.setSessionId(session.getName());
      willDetails.setVersion(protocol.getVersion());
      willDetails.setDelay(0);
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(payload);
      willDetails.setMsg(messageBuilder.build());
      session.updateWillTopic(willDetails);
    } else {
      task.updateMessage(payload);
    }
    return new WillMessageResponse(status);
  }
}