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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;

public class UnsubAck5 extends MQTTPacket5 {

  private final int packetId;

  public UnsubAck5(int packetId) {
    super(MQTTPacket.UNSUBACK);
    this.packetId = packetId;
  }

  @Override
  public String toString() {
    return "MQTTv5 UnsubAck[Packet Id:" + packetId + "]";
  }

  @Override
  public int packFrame(Packet packet) {
    int len = propertiesSize();
    packControlByte(packet, 0);
    packet.put((byte) (2 + len + lengthSize(len)));
    writeShort(packet, packetId);
    packProperties(packet, len);
    return 4;
  }
}
