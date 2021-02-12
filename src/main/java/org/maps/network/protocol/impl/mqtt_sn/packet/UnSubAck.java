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
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;

public class UnSubAck extends MQTT_SNPacket {

  private final int msgId;

  public UnSubAck(int msgId) {
    super(UNSUBACK);
    this.msgId = msgId;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 4);
    packet.put((byte) UNSUBACK);
    MQTTPacket.writeShort(packet, msgId);
    return 4;
  }

  @Override
  public String toString() {
    return "UnSubAck:MessageId:" + msgId;
  }

}
