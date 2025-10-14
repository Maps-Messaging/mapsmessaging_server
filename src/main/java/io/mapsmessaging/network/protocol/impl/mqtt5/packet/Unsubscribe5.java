/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Unsubscribe5 extends MQTTPacket5 {

  private final List<String> unsubscribeList;
  @Setter
  private int messageId;

  public Unsubscribe5(List<String> list){
    super(MQTTPacket.UNSUBSCRIBE);
    unsubscribeList = list;
  }


  // Between MQTT 3/4 and 5 there is duplicate code base, yes this is by design
  @java.lang.SuppressWarnings("common-java:DuplicatedBlocks")
  public Unsubscribe5(byte fixedHeader, long remainingLen, Packet packet)
      throws MalformedException, EndOfBufferException {
    super(MQTTPacket.UNSUBSCRIBE);
    if ((fixedHeader & 0xf) != 2) {
      throw new MalformedException(
          "Unsubscribe Fixed Header bits 3,2,1,0 must be set as 0,0,1,0  as per the specification :[MQTT-3.10.1-1]");
    }
    messageId = readShort(packet);
    long position = loadProperties(packet) + 2;
    unsubscribeList = new ArrayList<>();
    while (position < remainingLen) {
      String topicFilter = readUTF8(packet);
      unsubscribeList.add(topicFilter);
      position += topicFilter.length() + 2; // 2 for the length of the string
    }
    if (unsubscribeList.isEmpty()) {
      throw new MalformedException("Unsubscribe request must have at least 1 topic / qos entry as per [MQTT-3.10.3-2]");
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MQTTv5 Unsubscribe[");
    for (String info : unsubscribeList) {
      sb.append(" TopicName:").append(info);
    }
    sb.append("]");
    return sb.toString();
  }


  @Override
  public int packFrame(Packet packet) {
    int remainingLength = 2; // packet identifier

    remainingLength += 1; // properties length varint (0)

    for (String topicFilter : unsubscribeList) {
      int utf8Length = topicFilter.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
      remainingLength += 2 + utf8Length; // length prefix + bytes
    }

    packControlByte(packet, 2);
    writeVariableInt(packet, remainingLength);
    writeShort(packet, messageId);
    writeVariableInt(packet, 0); // properties length = 0

    for (String topicFilter : unsubscribeList) {
      writeUTF8(packet, topicFilter);
    }

    return remainingLength;
  }

}
