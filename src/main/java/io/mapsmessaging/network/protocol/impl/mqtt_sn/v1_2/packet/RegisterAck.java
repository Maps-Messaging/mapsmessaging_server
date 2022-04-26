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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import java.io.IOException;
import lombok.Getter;
import lombok.ToString;

@ToString
public class RegisterAck extends MQTT_SNPacket {

  @Getter
  private final int topicId;
  @Getter
  private final int messageId;
  @Getter
  private final ReasonCodes status;

  public RegisterAck(int topicId, int messageId, ReasonCodes status) {
    super(REGACK);
    this.topicId = topicId;
    this.messageId = messageId;
    this.status = status;
  }

  public RegisterAck(Packet packet, int length) throws IOException {
    super(REGACK);
    if (length < 7) {
      throw new IOException("Truncated packet received");
    }
    topicId = MQTTPacket.readShort(packet);
    messageId = MQTTPacket.readShort(packet);
    status = ReasonCodes.lookup(packet.get());
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 7);
    packet.put((byte) REGACK);
    MQTTPacket.writeShort(packet, topicId);
    MQTTPacket.writeShort(packet, messageId);
    packet.put((byte) status.getValue());
    return 7;
  }
}
