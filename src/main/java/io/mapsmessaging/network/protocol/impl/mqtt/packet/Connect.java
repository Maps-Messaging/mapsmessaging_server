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

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.security.uuid.UuidGenerator;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayOutputStream;

/**
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718028
 */
// Between MQTT 3/4 and 5 there is duplicate code base, yes this is by design
@java.lang.SuppressWarnings({"squid:S3776", "common-java:DuplicatedBlocks"})
@Getter
public class Connect extends MQTTPacket {

  private static final String NO_PROTOCOL_FOUND_MSG = "No Protocol string specified. [MQTT-3.1.2-1]";

  private static final byte[] MQTT311 = "MQTT".getBytes();
  private static final byte[] MQTT31 = "MQIsdp".getBytes();

  private byte protocolLevel;

  //
  // Will fields
  //
  @Setter
  private boolean willFlag;
  @Setter
  private QualityOfService willQOS;
  @Setter
  private boolean willRetain;
  @Setter
  private String willTopic;
  @Setter
  private byte[] willMsg;

  //
  // Username / Password fields
  //
  private boolean passwordFlag;
  private boolean usernameFlag;
  private String username;
  private char[] password;

  //
  // Session fields
  //
  @Setter
  private boolean cleanSession;
  @Setter
  private int keepAlive;
  @Setter
  private String sessionId;

  public Connect() {
    super(CONNECT);
  }


  public Connect(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException, EndOfBufferException {
    super(CONNECT);
    if ((fixedHeader & 0xf) != 0) {
      throw new MalformedException("Reserved Fixed header values set");
    }
    int packetSize = packet.limit() - packet.position();
    if (packetSize < remainingLen) {
      throw new EndOfBufferException("Connect Packet does not contain all the required data");
    }

    short len = (packet.get());
    len = (short) (len << 8);
    byte tmp = packet.get();
    len += tmp;
    if (len < MQTT311.length) {
      throw new MalformedException();
    }
    byte[] header = new byte[len];
    packet.get(header, 0, len);

    // Confirm we start with MQ
    for (int x = 0; x < 2; x++) {
      if (header[x] != MQTT311[x]) {
        throw new MalformedException(NO_PROTOCOL_FOUND_MSG);
      }
    }
    if (header[2] == MQTT31[2]) {
      for (int x = 2; x < len; x++) {
        if (header[x] != MQTT31[x]) {
          throw new MalformedException(NO_PROTOCOL_FOUND_MSG);
        }
      }
      protocolLevel = packet.get();
    } else if (header[2] == MQTT311[2]) {
      for (int x = 2; x < len; x++) {
        if (header[x] != MQTT311[x]) {
          throw new MalformedException(NO_PROTOCOL_FOUND_MSG);
        }
      }
      protocolLevel = packet.get();
    } else {
      throw new MalformedException(NO_PROTOCOL_FOUND_MSG);
    }

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
    passwordFlag = (connectFlag & 0x40) != 0; // Bit 6
    usernameFlag = (connectFlag & 0x80) != 0; // Bit 7

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
    keepAlive = readShort(packet) * 1000; // convert to ms
    String id = readUTF8(packet);

    if (id.isEmpty() && cleanSession) {
      id = UuidGenerator.getInstance().generate().toString();
    }
    sessionId = id;
    //
    // Will  topic and message if supplied
    //
    if (willFlag) {
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

  public void setUsername(String username) {
    this.username = username;
    usernameFlag = (username != null);
  }

  public void setPassword(char[] password) {
    this.password = password;
    passwordFlag = (password != null);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MQTT Connect [Protocol Level:").append(protocolLevel);
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
    sb.append("Username Flag:").append(usernameFlag);
    if (usernameFlag) {
      sb.append(" username:").append(username);
    }
    sb.append("Password Flag:").append(passwordFlag);
    if (passwordFlag) {
      sb.append(" Password Len:").append(password.length);
    }

    return sb.toString();
  }

  public int packFrame(Packet packet) {
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    b.write(0);
    b.write(MQTT311.length);
    b.write(MQTT311, 0, MQTT311.length);
    b.write(4);

    byte connectFlag = 0;
    if (usernameFlag) {
      connectFlag = (byte) (connectFlag + 0x80);
    }
    if (passwordFlag) {
      connectFlag = (byte) (connectFlag + 0x40);
    }
    b.write(connectFlag);

    // Keep Alive
    b.write(0);
    b.write((byte) 60);

    // Will never be greater then 2^31
    int size = 10 + sessionId.length() + 2;
    if (passwordFlag && usernameFlag) {
      size += password.length + username.length() + 4;
    }

    // Pack the header
    int start = packet.position();
    byte fixed = (byte) (CONNECT << 4);
    packet.put(fixed);
    writeVariableInt(packet, size);
    packet.put(b.toByteArray());
    writeUTF8(packet, sessionId);
    writeUTF8(packet, username);
    writeShort(packet, password.length);
    for (char c : password) {
      packet.put((byte) c);
    }
    return packet.position() - start;
  }
}
