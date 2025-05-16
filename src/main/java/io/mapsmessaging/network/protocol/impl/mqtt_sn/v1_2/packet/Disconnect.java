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

@ToString
public class Disconnect extends MQTT_SNPacket {

  @Getter
  private final int duration;

  public Disconnect(Packet packet, int length) {
    super(DISCONNECT);
    if (length >= 4) {
      duration = MQTTPacket.readShort(packet);
    } else {
      duration = 0;
    }
  }

  public Disconnect() {
    super(DISCONNECT);
    duration = 0;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 2);
    packet.put((byte) DISCONNECT);
    return 2;
  }
}
