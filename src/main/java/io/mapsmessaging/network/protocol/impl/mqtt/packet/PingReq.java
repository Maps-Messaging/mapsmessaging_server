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
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718081
 */
public class PingReq extends MQTTPacket {

  public PingReq() {
    super(PINGREQ);
  }

  public PingReq(byte fixedHeader, long remainingLen) throws MalformedException {
    super(MQTTPacket.PINGREQ);
    if ((fixedHeader & 0xf) != 0) {
      throw new MalformedException("PingReq: Reserved bits in command byte not 0");
    }
    if (remainingLen != 0) {
      throw new MalformedException("PingReq: remaining length not 0");
    }
  }

  @Override
  public int packFrame(Packet packet) {
    packControlByte(packet, 0);
    packet.put((byte) 0);
    return 2;
  }

  @Override
  public String toString() {
    return "MQTT PingReq[]";
  }
}
