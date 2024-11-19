/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import java.util.ArrayList;
import java.util.List;

public class Unsubscribe5 extends MQTTPacket5 {

  private final List<String> unsubscriptionList;
  private final int packetId;

  // Between MQTT 3/4 and 5 there is duplicate code base, yes this is by design
  @java.lang.SuppressWarnings("common-java:DuplicatedBlocks")
  public Unsubscribe5(byte fixedHeader, long remainingLen, Packet packet)
      throws MalformedException, EndOfBufferException {
    super(MQTTPacket.UNSUBSCRIBE);
    if ((fixedHeader & 0xf) != 2) {
      throw new MalformedException(
          "Unsubscribe Fixed Header bits 3,2,1,0 must be set as 0,0,1,0  as per the specification :[MQTT-3.10.1-1]");
    }
    packetId = readShort(packet);
    long position = loadProperties(packet) + 2;
    unsubscriptionList = new ArrayList<>();
    while (position < remainingLen) {
      String topicFilter = readUTF8(packet);
      unsubscriptionList.add(topicFilter);
      position += topicFilter.length() + 2; // 2 for the length of the string
    }
    if (unsubscriptionList.isEmpty()) {
      throw new MalformedException("Unsubscribe request must have at least 1 topic / qos entry as per [MQTT-3.10.3-2]");
    }
  }

  public int getPacketId() {
    return packetId;
  }

  public List<String> getUnsubscribeList() {
    return unsubscriptionList;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MQTTv5 Unsubscribe[");
    for (String info : unsubscriptionList) {
      sb.append(" TopicName:").append(info);
    }
    sb.append("]");
    return sb.toString();
  }

  public int packFrame(Packet packet) {
    return 0;
  }
}
