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

import java.io.IOException;
import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;

public class Publish extends MQTT_SNPacket {

  private final int topicId;
  private final int messageId;
  private final byte[] message;

  public Publish(short topicId, int messageId, byte[] message) {
    super(PUBLISH);
    this.topicId = topicId;
    this.messageId = messageId;
    this.message = message;
  }

  public Publish(Packet packet, int length) throws IOException {
    super(PUBLISH);
    flags = packet.get();
    topicId = MQTTPacket.readShort(packet);
    messageId = MQTTPacket.readShort(packet);
    if (length - 7 > packet.available()) {
      throw new IOException("Truncated packet received");
    }
    message = new byte[packet.available()];
    packet.get(message, 0, message.length);
  }

  public int getTopicId() {
    return topicId;
  }

  public int getMessageId() {
    return messageId;
  }

  public byte[] getMessage() {
    return message;
  }

  @Override
  public int packFrame(Packet packet) {
    int len = 7 + message.length;
    if (len < 256) {
      packet.put((byte) len);
    } else {
      packet.put((byte) 1);
      MQTTPacket.writeShort(packet, len);
    }
    packet.put((byte) PUBLISH);
    packet.put(flags);
    MQTTPacket.writeShort(packet, topicId);
    MQTTPacket.writeShort(packet, messageId);
    packet.put(message);
    return len;
  }

  @Override
  public String toString() {
    return "Publish:TopicId:" + topicId + " MessageId:" + messageId + " " + super.toString() + " OpaqueLength:" + message.length;
  }
}
