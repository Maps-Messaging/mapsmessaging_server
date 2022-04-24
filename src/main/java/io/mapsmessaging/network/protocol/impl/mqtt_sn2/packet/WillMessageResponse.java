/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn2.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import lombok.Getter;
import lombok.ToString;

@ToString
public class WillMessageResponse extends MQTT_SN_2_Packet {

  @Getter
  private final int status;

  public WillMessageResponse(int status) {
    super(WILLMSGRESP);
    this.status = status;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 4);
    packet.put((byte) WILLMSGRESP);
    MQTTPacket.writeShort(packet, status);
    return 4;
  }
}