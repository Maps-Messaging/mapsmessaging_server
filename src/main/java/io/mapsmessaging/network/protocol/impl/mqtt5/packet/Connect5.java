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

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901033
 */

// Between MQTT 3/4 and 5 there is duplicate code base, yes this is by design
@java.lang.SuppressWarnings("common-java:DuplicatedBlocks")
public class Connect5 extends MQTTPacket5 {

  private final byte[] mqtt = "MQTT".getBytes();

  private final byte protocolLevel;

  //
  // Will fields
  //
  @Getter
  private final MessageProperties willProperties;
  @Getter
  private final boolean willFlag;
  @Getter
  private final QualityOfService willQOS;
  @Getter
  private final boolean willRetain;
  @Getter
  private final String willTopic;
  @Getter
  private final byte[] willMsg;

  //
  // Username / Password fields
  //
  @Getter
  @Setter
  private String username;
  @Getter
  @Setter
  private char[] password;

  //
  // Session fields
  //
  @Getter
  private final boolean cleanSession;
  @Getter
  private final int keepAlive;

  @Getter
  @Setter
  private String sessionId;

  public Connect5(){
    super(CONNECT);
    protocolLevel = 5;
    cleanSession = false;
    keepAlive = 60;
    sessionId = null;
    willFlag = false;
    willRetain = false;
    willTopic = null;
    willMsg = null;
    willProperties = null;
    willQOS = QualityOfService.AT_MOST_ONCE;
  }

  // Due to the nature of a MQTT Version5 packet we validate as we go, else we can simply run off
  // the packet. This then causes the "complexity" score to be higher than configured. If could
  // be split but then that splits the logic of the connect packet itself which doesn't help
  // understanding of the logic.
  @java.lang.SuppressWarnings("squid:S3776")
  public Connect5(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException, EndOfBufferException {
    super(CONNECT);
    if ((fixedHeader & 0xf) != 0) {
      throw new MalformedException("Reserved Fixed header values set " + Long.toBinaryString(fixedHeader & 0xf));
    }
    int packetSize = packet.limit() - packet.position();
    if (packetSize < remainingLen) {
      throw new EndOfBufferException("Connect Packet does not contain all the required data");
    }

    short len = (packet.get());
    len = (short) (len << 8);
    byte tmp = packet.get();
    len += tmp;
    if (len < mqtt.length) {
      throw new MalformedException();
    }
    byte[] header = new byte[len];
    packet.get(header, 0, len);
    for (int x = 0; x < len; x++) {
      if (header[x] != mqtt[x]) {
        throw new MalformedException("No Protocol string specified. [MQTT-3.1.2-1]");
      }
    }
    protocolLevel = packet.get();

    // BYTE 8
    byte connectFlag = packet.get();
    if ((connectFlag & 0x1) != 0) {
      throw new MalformedException(
          "The Server MUST validate that the reserved flag in the CONNECT Control Packet is set to zero and disconnect the Client if it is not zero [MQTT-3.1.2-3]");
    }

    cleanSession = (connectFlag & 0x2) != 0; // Bit 1
    willFlag = (connectFlag & 0x4) != 0; // Bit 2
    willQOS = QualityOfService.getInstance((connectFlag & 0x18) >> 3); // Bit 3|4
    willRetain = (connectFlag & 0x20) != 0; // Bit 5
    boolean passwordFlag = (connectFlag & 0x40) != 0; // Bit 6
    boolean usernameFlag = (connectFlag & 0x80) != 0; // Bit 7
    if (!willFlag) {
      if (!willQOS.equals(QualityOfService.AT_MOST_ONCE)) {
        throw new MalformedException("If the Will Flag is set to 0, then the Will QoS MUST be set to 0 (0x00) [MQTT-3.1.2-13]");
      }
      if (willRetain) {
        throw new MalformedException("If the Will Flag is set to 0, then the Will Retain Flag MUST be set to 0 [MQTT-3.1.2-15]");
      }
    } else {
      if (willQOS.equals(QualityOfService.MQTT_SN_REGISTERED)) {
        throw new MalformedException("If the Will Flag is set to 1, the value of Will QoS can be 0 (0x00), 1 (0x01), or 2 (0x02). It MUST NOT be 3 (0x03) [MQTT-3.1.2-14]");
      }
    }

    //
    // BYTE 9, 10 ( Keep Alive )
    //
    keepAlive = readShort(packet);

    loadProperties(packet);

    sessionId = readUTF8(packet);

    //
    // Will  topic and message if supplied
    //
    willProperties = new MessageProperties();
    if (willFlag) {
      loadProperties(packet, willProperties);
      willTopic = readUTF8(packet);
      willMsg = readBuffer(packet);
    } else {
      willTopic = null;
      willMsg = null;
    }

    //
    // Username if supplied
    if (usernameFlag) {
      username = readUTF8(packet);
    } else {
      username = null;
    }

    //
    // Password if supplied
    //
    if (passwordFlag) {
      byte[] pswd = readBuffer(packet);
      password = new char[pswd.length];
      for (int x = 0; x < pswd.length; x++) {
        password[x] = (char) pswd[x];
      }
    } else {
      password = null;
    }
  }

  public boolean hasUsername(){
    return username != null && !username.isEmpty() && !username.isBlank();
  }

  public boolean hasPassword(){
    return password != null && password.length > 0;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MQTTv5 Connect [Protocol Level:").append(protocolLevel);
    sb.append(" SessionId:").append(sessionId).append(" Clean:").append(cleanSession);
    sb.append(" KeepAlive:").append(keepAlive);
    sb.append(" WillFlag:").append(willFlag);
    if (willFlag) {
      sb.append("< QoS")
          .append(willQOS)
          .append(" Topic")
          .append(willTopic)
          .append(" Length:")
          .append(willMsg.length)
          .append(" Retain:")
          .append(willRetain)
          .append(">");
    }
    sb.append("Username Flag:").append(hasUsername());
    if (hasUsername()) {
      sb.append(" username:").append(username);
    }
    sb.append("Password Flag:").append(hasPassword());
    if (hasPassword()) {
      sb.append(" Password Len:").append(password.length);
    }
    MessageProperties props = getProperties();
    for (MessageProperty property : props.values()) {
      sb.append(property.toString()).append(",");
    }
    return sb.toString();
  }

  @Override
  public int packFrame(Packet packet) {
    // ---- Variable header ----
    // Protocol Name "MQTT"
    int variableHeaderLength = 2 + mqtt.length;
    // Protocol Level
    variableHeaderLength += 1;
    // Connect Flags
    variableHeaderLength += 1;
    // Keep Alive
    variableHeaderLength += 2;

    int connectPropertiesLength = propertiesSize();
    int connectPropertiesVarIntSize = lengthSize(connectPropertiesLength);
    variableHeaderLength += connectPropertiesVarIntSize + connectPropertiesLength;

    // ---- Payload length calculation ----
    String clientIdentifier = (sessionId == null) ? "" : sessionId;
    int payloadLength = 2 + clientIdentifier.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;

    int willSectionLength = 0;
    if (willFlag) {
      int willPropsLen = propertiesSize(willProperties);
      int willPropsVarInt = lengthSize(willPropsLen);
      int willTopicLen = 2 + willTopic.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
      int willMsgLen = 2 + willMsg.length; // binary with 2-byte length
      willSectionLength = willPropsVarInt + willPropsLen + willTopicLen + willMsgLen;
      payloadLength += willSectionLength;
    }

    if (hasUsername()) {
      payloadLength += 2 + username.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    if (hasPassword()) {
      payloadLength += 2 + password.length; // binary with 2-byte length
    }

    int remainingLength = variableHeaderLength + payloadLength;

    // ---- Fixed header ----
    packControlByte(packet, 0);
    writeVariableInt(packet, remainingLength);

    // ---- Write variable header ----
    packet.putShort((short) mqtt.length);
    packet.put(mqtt);
    packet.put(protocolLevel);

    byte connectFlag = 0;
    if (cleanSession) {
      connectFlag |= 0b00000010;
    }
    if (willFlag) {
      connectFlag |= 0b00000100;
      connectFlag |= (byte) ((willQOS.getLevel() & 0b11) << 3);
      if (willRetain) {
        connectFlag |= 0b00100000;
      }
    }
    if (hasPassword()) {
      connectFlag |= 0b01000000;
    }
    if (hasUsername()) {
      connectFlag |= (byte) 0b10000000;
    }
    packet.put(connectFlag);

    packet.putShort((short) keepAlive);

    packProperties(packet, connectPropertiesLength);

    // ---- Write payload ----
    writeUTF8(packet, clientIdentifier);

    if (willFlag) {
      packProperties(packet, willProperties, propertiesSize(willProperties));
      writeUTF8(packet, willTopic);
      writeBuffer(willMsg, packet);
    }

    if (hasUsername()) {
      writeUTF8(packet, username);
    }

    if (hasPassword()) {
      byte[] passwordBytes = new byte[password.length];
      for (int index = 0; index < password.length; index++) {
        passwordBytes[index] = (byte) password[index];
      }
      writeBuffer(passwordBytes, packet);
    }

    return remainingLength;
  }


}
