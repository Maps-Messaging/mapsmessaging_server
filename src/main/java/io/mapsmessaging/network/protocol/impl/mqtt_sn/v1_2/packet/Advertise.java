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
public class Advertise extends MQTT_SNPacket {

  @Getter
  private final short gatewayId;
  @Getter
  private final int duration;

  public Advertise(byte id, short duration) {
    super(ADVERTISE);
    gatewayId = id;
    this.duration = duration;
  }

  public Advertise(Packet packet, int length) throws IOException {
    super(ADVERTISE);
    if (length < 5) {
      throw new IOException("Advertise Packet length less that the required size");
    }
    gatewayId = packet.get();
    duration = MQTTPacket.readShort(packet);
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 5);
    packet.put((byte) ADVERTISE);
    packet.put((byte) gatewayId);
    MQTTPacket.writeShort(packet, duration);
    return 5;
  }
}
