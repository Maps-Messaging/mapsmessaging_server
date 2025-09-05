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
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718053
 */
public class PubRel extends PublishMonitorPacket {

  public PubRel(int packetId) {
    super(PUBREL, 0b0010);
    this.packetId = packetId;
  }

  public PubRel(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException {
    super(PUBREL);
    if ((fixedHeader & 0xf) != 2) {
      throw new MalformedException(
          "PubRel: Reserved bits in command byte not set as 0,0,1,0 as per [MQTT-3.6.1-1]");
    }
    if (remainingLen != 2) {
      throw new MalformedException("PubRel: Remaining Length must be 2");
    }
    packetId = readShort(packet);
  }

  @Override
  public String toString() {
    return "MQTT PubRel[Packet Id:" + packetId + "]";
  }
}
