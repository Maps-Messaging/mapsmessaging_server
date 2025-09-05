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

public abstract class PublishMonitorPacket extends MQTTPacket {

  protected final int reservedBits;
  protected int packetId;

  protected PublishMonitorPacket(int id) {
    this(id, 0);
  }

  protected PublishMonitorPacket(int id, int reservedBits) {
    super(id);
    this.reservedBits = reservedBits;
  }

  public int getPacketIdentifier() {
    return packetId;
  }

  @Override
  public int packFrame(Packet packet) {
    super.packControlByte(packet, reservedBits);
    packet.put((byte) 2);
    if (packetId != -1) {
      packet.put((byte) (packetId >> 8 & 0xff));
      packet.put((byte) (packetId & 0xff));
    }
    return 4;
  }
}
