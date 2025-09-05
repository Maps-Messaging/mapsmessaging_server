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
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;

public class SubAck5 extends MQTTPacket5 {

  private final int packetId;
  private final StatusCode[] result;

  public SubAck5(int packetId, StatusCode[] result) {
    super(MQTTPacket.SUBACK);
    this.result = result;
    this.packetId = packetId;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MQTTv5 SubAck[ Result:");
    for (StatusCode b : result) {
      sb.append(b);
    }
    sb.append(" Packet Id:").append(packetId).append("]");
    return sb.toString();
  }

  @Override
  public int packFrame(Packet packet) {
    byte[] buf = new byte[result.length];
    int x = 0;
    for (StatusCode status : result) {
      buf[x] = status.getValue();
      x++;
    }
    int len = propertiesSize();
    packControlByte(packet, 0);
    packet.put((byte) (buf.length + 2 + len + lengthSize(len)));
    writeShort(packet, packetId);
    packProperties(packet, len);
    writeRawBuffer(buf, packet);
    return 4 + buf.length + len;
  }
}
