/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.mqtt.packet;

import java.util.UUID;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.network.io.Packet;
import org.maps.network.protocol.EndOfBufferException;

/**
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718028
 */
// Between MQTT 3/4 and 5 there is duplicate code base, yes this is by design
@java.lang.SuppressWarnings({"squid:S3776", "common-java:DuplicatedBlocks"})
public class Connect extends MQTTPacket {

  private final byte[] mqtt = "MQTT".getBytes();

  private final byte protocolLevel;

  //
  // Will fields
  //
  private final boolean willFlag;
  private final QualityOfService willQOS;
  private final boolean willRetain;
  private final String willTopic;
  private final byte[] willMsg;

  //
  // Username / Password fields
  //
  private final boolean passwordFlag;
  private final boolean usernameFlag;
  private final String username;
  private final char[] password;

  //
  // Session fields
  //
  private final boolean cleanSession;
  private final int keepAlive;
  private final String sessionId;

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
    int tmp1 = packet.get() << 8;
    tmp1 = tmp1 + packet.get();
    keepAlive = tmp1 * 1000; // convert to ms

    String id = readUTF8(packet);

    if (id.length() == 0 && cleanSession) {
      id = UUID.randomUUID().toString();
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

  public boolean isPasswordFlag() {
    return passwordFlag;
  }

  public boolean isUsernameFlag() {
    return usernameFlag;
  }

  public byte getProtocolLevel() {
    return protocolLevel;
  }

  public int getKeepAlive() {
    return keepAlive;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getWillTopic() {
    return willTopic;
  }

  public byte[] getWillMsg() {
    return willMsg;
  }

  public String getUsername() {
    return username;
  }

  public char[] getPassword() {
    return password;
  }

  public boolean isCleanSession() {
    return cleanSession;
  }

  public boolean isWillFlag() {
    return willFlag;
  }

  public QualityOfService getWillQOS() {
    return willQOS;
  }

  public boolean isWillRetain() {
    return willRetain;
  }

  public char[] isPassword() {
    return password;
  }

  public String isUsername() {
    return username;
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
    return 0;
  }
}
