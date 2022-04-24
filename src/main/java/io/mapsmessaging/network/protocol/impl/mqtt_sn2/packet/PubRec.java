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

package io.mapsmessaging.network.protocol.impl.mqtt_sn2.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import lombok.Getter;
import lombok.ToString;

@ToString
public class PubRec extends MQTT_SN_2_Packet {

  @Getter
  private final int messageId;

  public PubRec(int messageId) {
    super(PUBREC);
    this.messageId = messageId;
  }

  public PubRec(Packet packet) {
    super(PUBREC);
    messageId = MQTTPacket.readShort(packet);
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 4);
    packet.put((byte) PUBREC);
    MQTTPacket.writeShort(packet, messageId);
    return 4;
  }
}
