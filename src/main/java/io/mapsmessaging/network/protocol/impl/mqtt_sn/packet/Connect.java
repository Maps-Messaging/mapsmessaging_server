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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;

public class Connect extends MQTT_SNPacket {

  private final short protocolId;
  private final int duration;
  private final String clientId;

  public Connect(Packet packet, int length) {
    super(CONNECT);
    flags = packet.get();
    protocolId = packet.get();
    duration = MQTTPacket.readShort(packet);
    byte[] tmp = new byte[length - 6];
    packet.get(tmp, 0, tmp.length);
    clientId = new String(tmp);
  }

  public short getProtocolId() {
    return protocolId;
  }

  public int getDuration() {
    return duration;
  }

  public String getClientId() {
    return clientId;
  }

  @Override
  public String toString() {
    return "Connect: Id:" + clientId + " ProtocolId:" + protocolId + " Duration:" + duration + " " + super.toString();
  }
}
