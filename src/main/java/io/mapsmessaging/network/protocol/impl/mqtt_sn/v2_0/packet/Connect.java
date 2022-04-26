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
import java.io.IOException;
import lombok.Getter;
import lombok.ToString;

@ToString
public class Connect extends MQTT_SN_2_Packet {

  @Getter
  private final short protocolId;
  @Getter
  private final int keepAlive;
  @Getter
  private final int maxPacketSize;
  @Getter
  private final long sessionExpiry;
  @Getter
  private final String clientId;
  @Getter
  private final boolean cleanStart;
  @Getter
  private final boolean will;
  @Getter
  private final boolean authentication;

  public Connect(Packet packet, int length) throws IOException {
    super(CONNECT);
    byte val = packet.get();
    cleanStart = (val & 0b00000001) != 0;
    will = (val & 0b00000010) != 0;
    authentication =(val & 0b00000100) != 0;
    if( val>>3 != 0){
      throw new IOException("3.1.4.2 - Malformed Packet received");
    }
    protocolId = packet.get();
    keepAlive = MQTTPacket.readShort(packet);
    sessionExpiry = MQTTPacket.readInt(packet);
    maxPacketSize = MQTTPacket.readShort(packet);

    byte[] tmp = new byte[packet.available()];
    packet.get(tmp, 0, tmp.length);
    clientId = new String(tmp);
  }
}
