/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UnsubAck5 extends MQTTPacket5 {

  private int packetId;
  private final List<StatusCode> reasonCodes;

  public UnsubAck5(int packetId) {
    this(packetId, new StatusCode[]{StatusCode.SUCCESS});
  }

  public UnsubAck5(int packetId, StatusCode[] reasons) {
    super(MQTTPacket.UNSUBACK);
    if (reasons == null || reasons.length == 0) {
      throw new IllegalArgumentException("UNSUBACK requires at least one reason code");
    }
    this.packetId = packetId;
    reasonCodes = Collections.unmodifiableList(Arrays.asList(reasons.clone()));
  }

  public int getPacketId() {
    return packetId;
  }

  public List<StatusCode> getReasonCodes() {
    return reasonCodes;
  }

  public UnsubAck5(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException, EndOfBufferException {
    super(MQTTPacket.UNSUBACK);
    if ((fixedHeader & 0x0F) != 0) {
      throw new MalformedException("UnsubAck: Reserved bits in command byte not 0");
    }

    // Variable header
    packetId = readShort(packet);
    long propsBytes = loadProperties(packet); // includes the properties length varint itself

    long payloadLen = remainingLen - 2 - propsBytes; // 2 = packetId
    if (payloadLen < 0) {
      throw new MalformedException("UnsubAck malformed: remaining length too small");
    }

    if (payloadLen == 0 || payloadLen > Integer.MAX_VALUE) {
      throw new MalformedException("UNSUBACK requires at least one reason code");
    }
    List<StatusCode> reasons = new ArrayList<>((int) payloadLen);
    for (int i = 0; i < payloadLen; i++) {
      byte value = packet.get();
      StatusCode reason;
      try {
        reason = StatusCode.getInstance(value);
      } catch (IllegalArgumentException unknown) {
        throw new MalformedException("Unknown UNSUBACK reason code " + (value & 0xff));
      }
      if (!isAllowedReason(reason)) {
        throw new MalformedException("Invalid UNSUBACK reason code " + (value & 0xff));
      }
      reasons.add(reason);
    }
    reasonCodes = Collections.unmodifiableList(reasons);
  }

  private static boolean isAllowedReason(StatusCode reason) {
    return reason == StatusCode.SUCCESS || reason == StatusCode.NO_SUBSCRIPTION_EXISTED
        || reason == StatusCode.UNSPECIFIED_ERROR
        || reason == StatusCode.IMPLEMENTATION_SPECIFIC_ERROR
        || reason == StatusCode.NOT_AUTHORISED || reason == StatusCode.TOPIC_FILTER_INVALID
        || reason == StatusCode.PACKET_IDENTIFIER_INUSE;
  }

  @Override
  public String toString() {
    return "MQTTv5 UnsubAck[Packet Id:" + packetId + " Reasons:" + reasonCodes + "]";
  }

  @Override
  public int packFrame(Packet packet) {
    int propertiesLength = propertiesSize();
    int remaining = 2 + propertiesLength + lengthSize(propertiesLength) + reasonCodes.size();
    int start = packet.position();
    packControlByte(packet, 0);
    writeVariableInt(packet, remaining);
    writeShort(packet, packetId);
    packProperties(packet, propertiesLength);
    for (StatusCode reason : reasonCodes) {
      packet.put(reason.getValue());
    }
    return packet.position() - start;
  }
}
