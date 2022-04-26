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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import lombok.Getter;
import lombok.ToString;

@ToString
public class Disconnect extends MQTT_SN_2_Packet {

  @Getter
  private final long expiry;
  @Getter
  private final ReasonCodes reasonCode;
  @Getter
  private final String reasonString;

  public Disconnect(Packet packet, int length) {
    super(DISCONNECT);
    if (length > 2) {
      reasonCode = ReasonCodes.lookup(packet.get());
      expiry = MQTTPacket.readInt(packet);
      byte[] tmp = new byte[length - 8];
      packet.get(tmp, 0, tmp.length);
      reasonString = new String(tmp);
    } else {
      expiry = 0;
      reasonString="";
      reasonCode = ReasonCodes.Success;
    }
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 2);
    packet.put((byte) DISCONNECT);
    return 2;
  }
}