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
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;

public class PubRec5 extends PublishMonitorPacket5 {

  public PubRec5(int packetId) {
    super(PUBREC);
    this.packetId = packetId;
  }

  public PubRec5(byte fixedHeader, long remainingLen, Packet packet)
      throws MalformedException, EndOfBufferException {
    super(fixedHeader, remainingLen, packet);
    if ((fixedHeader & 0xf) != 0) {
      throw new MalformedException("PubComp: Reserved bits in command byte not 0");
    }
  }

  @Override
  public String toString() {
    return "MQTTv5 PubRec[Packet Id:" + packetId + "]";
  }
}
