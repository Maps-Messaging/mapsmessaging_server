/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.ToString;

@ToString
public class SearchGateway extends MQTT_SNPacket {

  @Getter
  private final short radius;

  public SearchGateway(short radius) {
    super(SEARCHGW);
    this.radius = radius;
  }

  public SearchGateway(Packet packet) {
    super(SEARCHGW);
    radius = packet.get();
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 3);
    packet.put((byte) SEARCHGW);
    packet.put((byte) radius);
    return 3;
  }
}
