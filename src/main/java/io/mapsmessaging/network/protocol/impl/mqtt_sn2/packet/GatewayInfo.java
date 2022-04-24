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
import lombok.Getter;
import lombok.ToString;

@ToString
public class GatewayInfo extends MQTT_SN_2_Packet {

  @Getter
  private final short gatewayId;
  @Getter
  private final byte[] gatewayAddress;

  public GatewayInfo(short gatewayId, byte[] gatewayAddress) {
    super(GWINFO);
    this.gatewayId = gatewayId;
    this.gatewayAddress = gatewayAddress;
  }

  public GatewayInfo(Packet packet, int length) {
    super(GWINFO);
    gatewayId = packet.get();
    byte[] tmp = new byte[length - 3];
    packet.get(tmp, 0, tmp.length);
    gatewayAddress = tmp;
  }

  @Override
  public int packFrame(Packet packet) {
    int len = 3;
    if(gatewayAddress != null){
      len = 3+ gatewayAddress.length;
    }
    packet.put((byte) len);
    packet.put((byte) GWINFO);
    packet.put((byte) gatewayId);
    if(gatewayAddress != null){
      packet.put(gatewayAddress);
    }
    return len;
  }
}