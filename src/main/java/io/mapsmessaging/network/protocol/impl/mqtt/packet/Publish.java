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

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPublishPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.DefaultConstants;
import lombok.Getter;

import java.nio.ByteBuffer;

/**
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718037
 */

@java.lang.SuppressWarnings({"common-java:DuplicatedBlocks"})
public class Publish extends MQTTPacket implements ServerPublishPacket {

  @Getter
  private final boolean retain;
  private final boolean isDup;
  @Getter
  private final QualityOfService qos;
  @Getter
  private final String destinationName;
  @Getter
  private final int packetId;
  @Getter
  private final byte[] payload;

  public Publish(boolean retain, byte[] payload, QualityOfService qos, int packetId, String destination) {
    super(PUBLISH);
    this.payload = payload != null ? payload : new byte[0];
    this.retain = retain;
    isDup = false;
    this.qos = qos;
    this.packetId = packetId;
    destinationName = destination;
  }

  public Publish(byte fixedHeader, long remainingLen, Packet packet, long maximumBufferSize)
      throws MalformedException {
    super(PUBLISH);
    retain = (fixedHeader & 1) != 0;
    qos = QualityOfService.getInstance(((fixedHeader >> 1) & 3));
    isDup = (fixedHeader & 8) != 0;

    destinationName = readUTF8(packet);
    int nonData = destinationName.length() + 2;

    if (qos.isSendPacketId()) {
      nonData += 2;
      packetId = readShort(packet);
    } else {
      packetId = 0;
    }
    long payloadSize = (remainingLen - nonData);
    if (maximumBufferSize > 0 && payloadSize > maximumBufferSize) {
      throw new MalformedException("Payload size " + payloadSize + " exceeding configured maximum of " + maximumBufferSize);
    }
    payload = new byte[(int) payloadSize];
    packet.get(payload, 0, payload.length);

    if (qos.equals(QualityOfService.AT_MOST_ONCE) && isDup) {
      throw new MalformedException("Duplicate flag must be set to 0 if QoS is 0 as per [MQTT-3.3.1-2]");
    }
    if (qos.equals(QualityOfService.MQTT_SN_REGISTERED)) {
      throw new MalformedException("QoS must be 0, 1 or 2 only Reference: [MQTT-3.3.1-4]");
    }
    if (destinationName.isEmpty()) {
      throw new MalformedException("Topic name must be present Reference: [MQTT-3.3.2-1]");
    }
    if (!topicAllowed(destinationName) ||
        destinationName.contains("..") ||
        destinationName.contains("//") ||
        destinationName.toLowerCase().startsWith("$sys")) {
      throw new MalformedException("Destination name must not contain wildcards Reference: [MQTT-3.3.2-2]");
    }
  }

  boolean topicAllowed(String topicName) {
    return topicName.chars().noneMatch(c -> (c <= 0x1f || (c >= 0x7f && c <= 0x9f)) || (c == '#' || (c == '+')));
  }


  public boolean isDuplicate() {
    return isDup;
  }

  public Priority getPriority() {
    return DefaultConstants.PRIORITY;
  }

  @Override
  public String toString() {
    return "MQTT Publish[Destination:"
        + destinationName
        + " QoS:"
        + qos
        + " Retain:"
        + retain
        + "Packet Id:"
        + packetId
        + " Payload Size:"
        + payload.length
        + "]";
  }

  private long packHeader(Packet packet){
    //
    // Pack the header
    //
    byte fixed = (byte) (PUBLISH << 4);
    if (isDup) {
      fixed = (byte) ((fixed & 0xff) | (1 << 3));
    }
    fixed = (byte) ((fixed & 0xff) | ((qos.getLevel() & 0x3) << 1));
    if (retain) {
      fixed = (byte) (fixed + 0x1);
    }
    packet.put(fixed);
    long remaining = destinationName.length() + 2L;
    if (qos.isSendPacketId()) {
      remaining += 2;
    }
    remaining += (payload != null ? payload.length : 0);

    writeVariableInt(packet, remaining);
    writeUTF8(packet, destinationName);
    if (qos.isSendPacketId()) {
      writeShort(packet, packetId);
    }
    return remaining;
  }

  @java.lang.SuppressWarnings({"common-java:DuplicatedBlocks"})
  @Override
  public int packFrame(Packet packet) {
    long remaining = packHeader(packet);
    packet.put(payload);
    return (int) (remaining + 1);
  }

  @Override
  public Packet[] packAdvancedFrame(Packet packet) {
    packHeader(packet);
    if(payload.length < packet.available()) {
      packet.put(payload);
      return new Packet[]{packet};
    }
    Packet payloadPacket = new Packet(ByteBuffer.wrap(payload));
    return new Packet[]{packet, payloadPacket };
  }
}
