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

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.network.io.Packet;

/**
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718068
 */
public class SubAck extends MQTTPacket {

  private final int packetId;
  private final byte[] result;

  public SubAck(int packetId, byte[] result) {
    super(SUBACK);
    this.result = result;
    this.packetId = packetId;
  }

  public int getPacketId() {
    return packetId;
  }

  public byte[] getResult() {
    return result;
  }

  public SubAck(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException {
    super(SUBACK);
    if ((fixedHeader & 0xf) != 0) {
      throw new MalformedException("SubAck: Reserved bits in command byte not set as 0,0,0,0 as per [MQTT-3.6.1-1]");
    }
    if (remainingLen <= 2) {
      throw new MalformedException("SubAck: Remaining Length must be greater than 2");
    }
    packetId = readShort(packet);
    result = new byte[(int) remainingLen - 2];
    packet.get(result);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MQTT SubAck[ Result:");
    for (byte b : result) {
      if (b == -1) {
        sb.append("Error");
      } else {
        sb.append(b);
      }
    }
    sb.append(" Packet Id:").append(packetId).append("]");
    return sb.toString();
  }

  @Override
  public int packFrame(Packet packet) {
    packControlByte(packet, 0);
    packet.put((byte) (result.length + 2));
    writeShort(packet, packetId);
    writeRawBuffer(result, packet);
    return 4 + result.length;
  }
}
