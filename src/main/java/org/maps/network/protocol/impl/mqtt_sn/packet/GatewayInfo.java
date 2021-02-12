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

package org.maps.network.protocol.impl.mqtt_sn.packet;

import org.maps.network.io.Packet;

public class GatewayInfo extends MQTT_SNPacket {

  private final short gatewayId;

  public GatewayInfo(short gatewayId) {
    super(GWINFO);
    this.gatewayId = gatewayId;
  }

  public GatewayInfo(Packet packet) {
    super(GWINFO);
    gatewayId = packet.get();
  }

  public short getGatewayId() {
    return gatewayId;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 3);
    packet.put((byte) GWINFO);
    packet.put((byte) gatewayId);
    return 3;
  }

  @Override
  public String toString() {
    return "GatewayInfo:GatewayId:" + gatewayId;
  }
}
