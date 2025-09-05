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
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718048
 */
public class PubRec extends PublishMonitorPacket {

  public PubRec(int packetId) {
    super(PUBREC);
    this.packetId = packetId;
  }

  public PubRec(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException {
    super(PUBREC);
    if ((fixedHeader & 0xf) != 0) {
      throw new MalformedException("PubRec: Reserved bits in command byte not 0");
    }
    if (remainingLen != 2) {
      throw new MalformedException("PubRec: Remaining Length must be 2");
    }
    packetId = readShort(packet);
  }

  @Override
  public String toString() {
    return "MQTT PubRec[Packet Id:" + packetId + "]";
  }
}
