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

public class WillTopic extends MQTT_SNPacket {

  private final String topic;

  public WillTopic(Packet packet, int length) {
    super(WILLTOPIC);
    flags = packet.get();
    byte[] topicBuffer = new byte[length - 3];
    packet.get(topicBuffer, 0, topicBuffer.length);
    topic = new String(topicBuffer);
  }

  public String getTopic() {
    return topic;
  }

  @Override
  public int packFrame(Packet packet) {
    int len = 3 + topic.length();
    packet.put((byte) len);
    packet.put((byte) WILLTOPIC);
    packet.put(flags);
    packet.put(topic.getBytes());
    return 3 + topic.length();
  }

  @Override
  public String toString() {
    return "WillTopic:Topic:" + topic + " " + super.toString();
  }
}
