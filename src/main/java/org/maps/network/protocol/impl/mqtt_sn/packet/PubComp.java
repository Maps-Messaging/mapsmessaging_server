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

public class PubComp extends MQTT_SNPacket {

  private final int messageId;

  public PubComp(int messageId) {
    super(PUBCOMP);
    this.messageId = messageId;
  }

  public PubComp(Packet packet) {
    super(PUBCOMP);
    messageId = MQTTPacket.readShort(packet);
  }

  public int getMessageId() {
    return messageId;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 4);
    packet.put((byte) PUBCOMP);
    MQTTPacket.writeShort(packet, messageId);
    return 4;
  }

  @Override
  public String toString() {
    return "PubComp:MessageId:" + messageId;
  }

}
