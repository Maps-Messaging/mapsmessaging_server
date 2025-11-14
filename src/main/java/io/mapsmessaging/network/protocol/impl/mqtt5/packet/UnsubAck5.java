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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;

public class UnsubAck5 extends MQTTPacket5 {

  private int packetId;

  public UnsubAck5(int packetId) {
    super(MQTTPacket.UNSUBACK);
    this.packetId = packetId;
  }

  public UnsubAck5(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException, EndOfBufferException {
    super(MQTTPacket.UNSUBACK);
    if ((fixedHeader & 0x0F) != 0) {
      throw new MalformedException("UnsubAck: Reserved bits in command byte not 0");
    }

    // Variable header
    packetId = readShort(packet);
    long propsBytes = loadProperties(packet); // includes the properties length varint itself

    long payloadLen = remainingLen - 2 - propsBytes; // 2 = packetId
    if (payloadLen < 0) {
      throw new MalformedException("UnsubAck malformed: remaining length too small");
    }

    // Payload (optional in v5): may contain one or more reason codes
    if (payloadLen > 0) {
      for (int i = 0; i < payloadLen; i++) {
        byte reason = packet.get();
        // You can store reason codes if needed later
      }
    }
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
