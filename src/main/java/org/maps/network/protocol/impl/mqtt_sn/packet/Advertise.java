/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.mqtt_sn.packet;

import java.io.IOException;
import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;

public class Advertise extends MQTT_SNPacket {

  private final short gatewayId;
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

  @Override
  public String toString() {
    return "Advertise: GatewayId:" + gatewayId + " Interval:" + duration;
  }
}
