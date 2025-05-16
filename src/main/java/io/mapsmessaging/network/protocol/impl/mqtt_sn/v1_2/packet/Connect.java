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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;

@ToString
public class Connect extends MQTT_SNPacket {

  @Getter
  private final short protocolId;
  @Getter
  private final int duration;
  @Getter
  private final String clientId;

  private final byte flags;

  public Connect(Packet packet, int length) throws IOException {
    super(CONNECT);
    flags = packet.get();
    if ((flags & 0b11110011) != 0) {
      throw new IOException("Malformed flags");
    }
    protocolId = packet.get();
    duration = MQTTPacket.readShort(packet);
    byte[] tmp = new byte[length - 6];
    packet.get(tmp, 0, tmp.length);
    clientId = new String(tmp);
  }

  public boolean dup() {
    return (flags & 0b10000000) != 0;
  }

  public boolean will() {
    return (flags & 0b00001000) != 0;
  }

  public boolean clean() {
    return (flags & 0b00000100) != 0;
  }
}
