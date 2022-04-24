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
public class WillTopicUpdate extends MQTT_SN_2_Packet {

  @Getter
  private final String topic;
  @Getter
  private final boolean retain;
  @Getter
  private final int QoS;

  public WillTopicUpdate(Packet packet, int length) {
    super(WILLTOPICUPD);
    byte flags = packet.get();
    QoS =(flags & 0b01100000) >> 5;
    retain = (flags & 0b00010000) != 0;

    byte[] topicBuffer = new byte[length - 3];
    packet.get(topicBuffer, 0, topicBuffer.length);
    topic = new String(topicBuffer);
  }
}
