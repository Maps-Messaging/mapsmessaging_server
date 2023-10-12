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

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.network.io.Packet;

/**
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718058
 */
public class PubComp extends PublishMonitorPacket {

  public PubComp(int packetId) {
    super(PUBCOMP);
    this.packetId = packetId;
  }

  public PubComp(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException {
    super(PUBCOMP);
    if ((fixedHeader & 0xf) != 0) {
      throw new MalformedException("PubComp: Reserved bits in command byte not 0");
    }
    if (remainingLen != 2) {
      throw new MalformedException("PubComp: Remaining Length must be 2");
    }
    packetId = readShort(packet);
  }

  @Override
  public String toString() {
    return "MQTT PubComp[Packet Id:" + packetId + "]";
  }
}
