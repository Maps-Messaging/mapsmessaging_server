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

/**
 * https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901074
 */
public class ConnAck5 extends StatusPacket {

  private boolean isPresent;

  public ConnAck5() {
    super(CONNACK);
  }

  @Override
  public int packFrame(Packet packet) {
    int len = propertiesSize();
    packControlByte(packet, 0);
    writeVariableInt(packet, (2L + len + lengthSize(len)));

    if (isPresent) {
      packet.put((byte) 1);
    } else {
      packet.put((byte) 0);
    }
    packet.put(statusCode.getValue());
    packProperties(packet, len);
    return (len + 2);
  }

  public void setRestoredFlag(boolean restored) {
    isPresent = restored;
  }

  @Override
  public String toString() {
    return "MQTTv5 ConAck:: isPresent:" + isPresent + " " + super.toString();
  }
}
