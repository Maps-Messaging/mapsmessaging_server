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
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;

public abstract class PublishMonitorPacket5 extends StatusPacket {

  protected final int reservedBits;
  protected int packetId;

  protected PublishMonitorPacket5(int id) {
    this(id, 0);
  }

  protected PublishMonitorPacket5(int id, int reservedBits) {
    super(id);
    this.reservedBits = reservedBits;
  }

  protected PublishMonitorPacket5(byte fixedHeader, long remainingLen, Packet packet)
      throws MalformedException, EndOfBufferException {
    super(fixedHeader >> 4);
    reservedBits = fixedHeader & 0xf;
    packetId = readShort(packet);
    if (remainingLen > 2) {
      statusCode = StatusCode.getInstance(packet.get());
      if (remainingLen > 3) {
        loadProperties(packet);
      }
    }
  }

  public int getPacketIdentifier() {
    return packetId;
  }

  @Override
  public int packFrame(Packet packet) {
    int len = propertiesSize();
    super.packControlByte(packet, reservedBits);
    writeVariableInt(packet, 3L + len + lengthSize(len));
    if (packetId != -1) {
      packet.put((byte) (packetId >> 8 & 0xff));
      packet.put((byte) (packetId & 0xff));
    }
    packet.put(statusCode.getValue());
    packProperties(packet, len);
    return 4;
  }
}
