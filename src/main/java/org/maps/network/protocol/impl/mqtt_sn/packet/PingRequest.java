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

import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;

public class PingRequest extends MQTT_SNPacket {

  private int clientId;

  public PingRequest() {
    super(PINGREQ);
  }

  public PingRequest(Packet packet, int length) {
    super(PINGREQ);
    if (length > 2) {
      clientId = MQTTPacket.readShort(packet);
    }
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 2);
    packet.put((byte) PINGREQ);
    return 2;
  }

  @Override
  public String toString() {
    return "PingRequest:ClientId:" + clientId;
  }
}
