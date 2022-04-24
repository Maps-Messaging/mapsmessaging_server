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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import lombok.Getter;
import lombok.ToString;

@ToString
public class SubAck extends MQTT_SNPacket {

  @Getter
  private final int msgId;
  @Getter
  private final int topicId;
  @Getter
  private final byte status;

  public SubAck(short topicId, int msgId, byte status) {
    super(SUBACK);
    this.msgId = msgId;
    this.topicId = topicId;
    this.status = status;
    this.flags = 0;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 8);
    packet.put((byte) SUBACK);
    packet.put(flags); // Only QOS counts
    MQTTPacket.writeShort(packet, topicId);
    MQTTPacket.writeShort(packet, msgId);
    packet.put(status);
    return 8;
  }
}
