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

package org.maps.network.protocol.impl.mqtt5.packet;

import java.util.Collection;
import org.maps.messaging.api.features.Priority;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.network.io.Packet;
import org.maps.network.protocol.EndOfBufferException;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;
import org.maps.network.protocol.impl.mqtt5.DefaultConstants;
import org.maps.network.protocol.impl.mqtt5.packet.properties.MessageProperty;

/**
 * https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901100
 */
// Between MQTT 3/4 and 5 there is duplicate code base, yes this is by design
@java.lang.SuppressWarnings("common-java:DuplicatedBlocks")
public class Publish5 extends MQTTPacket5 {

  private final boolean retain;
  private final boolean isDup;
  private final QualityOfService qos;
  private final String destinationName;
  private final int packetId;
  private final byte[] payload;

  public Publish5(byte[] payload, QualityOfService qos, int packetId, String destination, boolean retain) {
    super(PUBLISH);
    this.payload = payload;
    this.retain = retain;
    isDup = false;
    this.qos = qos;
    this.packetId = packetId;
    destinationName = destination;
  }

  public Publish5(byte fixedHeader, long remainingLen, Packet packet, long maximumBufferSize)
      throws MalformedException, EndOfBufferException {
    super(PUBLISH);
    retain = (fixedHeader & 1) != 0;
    qos = QualityOfService.getInstance((fixedHeader >> 1) & 3);
    isDup = (fixedHeader & 8) != 0;

    if (qos.equals(QualityOfService.AT_MOST_ONCE) && isDup) {
      throw new MalformedException("Duplicate flag must be set to 0 if QoS is 0 as per [MQTT-3.3.1-2]");
    }
    if (qos.equals(QualityOfService.MQTT_SN_REGISTERED)) {
      throw new MalformedException("QoS must be 0, 1 or 2 only Reference: [MQTT-3.3.1-4]");
    }

    destinationName = readUTF8(packet);
    int nonData = destinationName.length() + 2;

    if (qos.isSendPacketId()) {
      nonData += 2;
      packetId = readShort(packet);
    } else {
      packetId = 0;
    }
    nonData += loadProperties(packet);
    long payloadSize = (remainingLen - nonData);
    if (maximumBufferSize > 0 && payloadSize > maximumBufferSize) {
      throw new MalformedException("Payload size " + payloadSize + " exceeding configured maximum of " + maximumBufferSize);
    }

    payload = new byte[(int) payloadSize];
    packet.get(payload, 0, payload.length);

    if (destinationName.contains("#") || destinationName.contains("+")) {
      throw new MalformedException("Destination name must not contain wildcards Reference: [MQTT-3.3.2-2]");
    }
  }

  public boolean isRetain() {
    return retain;
  }

  public boolean isDuplicate() {
    return isDup;
  }

  public String getDestinationName() {
    return destinationName;
  }

  public int getPacketId() {
    return packetId;
  }

  public byte[] getPayload() {
    return payload;
  }

  public QualityOfService getQos() {
    return qos;
  }

  public Priority getPriority() {
    return DefaultConstants.PRIORITY;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MQTTv5 Publish[Destination:")
        .append(destinationName)
        .append(" QoS:")
        .append(qos)
        .append(" Retain:")
        .append(retain)
        .append(" Packet Id:")
        .append(packetId)
        .append(" Payload Size:")
        .append(payload.length)
        .append("]");

    sb.append("<Properties: ");
    Collection<MessageProperty> propertyList = getProperties().values();
    for (MessageProperty messageProperty : propertyList) {
      sb.append(messageProperty.toString()).append(", ");
    }
    sb.append(">");
    return sb.toString();
  }

  @Override
  public int packFrame(Packet packet) {
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
    int len = propertiesSize();
    packet.put(fixed);
    long remaining = destinationName.length() + 2L + len + lengthSize(len);
    if (qos.isSendPacketId()) {
      remaining += 2;
    }
    remaining += payload.length;

    writeVariableInt(packet, remaining);
    writeUTF8(packet, destinationName);
    if (qos.isSendPacketId()) {
      writeShort(packet, packetId);
    }
    packProperties(packet, len);
    packet.put(payload);
    return (int) (remaining);
  }
}
