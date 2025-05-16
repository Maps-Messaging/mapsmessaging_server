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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.SocketAddress;

// http://www.mqtt.org/new/wp-content/uploads/2009/06/MQTT-SN_spec_v1.2.pdf
@java.lang.SuppressWarnings("squid:S00101")
@ToString
public class MQTT_SNPacket implements ServerPacket {

  public static final short TOPIC_NAME = 0;
  public static final short TOPIC_PRE_DEFINED_ID = 1;
  public static final short TOPIC_SHORT_NAME = 2;
  public static final short LONG_TOPIC_NAME = 3;

  public static final int ADVERTISE = 0x0;
  public static final int SEARCHGW = 0x1;
  public static final int GWINFO = 0x2;
  // 0x3 - Reserved
  public static final int CONNECT = 0x4;
  public static final int CONNACK = 0x5;

  public static final int WILLTOPICREQ = 0x6;
  public static final int WILLTOPIC = 0x7;

  public static final int WILLMSGREQ = 0x8;
  public static final int WILLMSG = 0x9;

  public static final int REGISTER = 0xA;
  public static final int REGACK = 0xB;

  public static final int PUBLISH = 0xC;
  public static final int PUBACK = 0xD;

  public static final int PUBCOMP = 0xE;
  public static final int PUBREC = 0xF;

  public static final int PUBREL = 0x10;
  // 0x11 reserved
  public static final int SUBSCRIBE = 0x12;
  public static final int SUBACK = 0x13;

  public static final int UNSUBSCRIBE = 0x14;
  public static final int UNSUBACK = 0x15;

  public static final int PINGREQ = 0x16;
  public static final int PINGRESP = 0x17;

  public static final int DISCONNECT = 0x18;
  // 0x19 reserved
  public static final int WILLTOPICUPD = 0x1A;
  public static final int WILLTOPICRESP = 0x1B;

  public static final int WILLMSGUPD = 0x1C;
  public static final int WILLMSGRESP = 0x1D;

  public static final int ENCAPSULATED = 0xFE;

  @Getter
  @Setter
  protected int controlPacketId;

  @Getter
  @Setter
  private SocketAddress fromAddress;

  @Getter
  @Setter
  private Runnable callback;

  public MQTT_SNPacket(int id) {
    this.controlPacketId = id;
  }

  @Override
  public int packFrame(Packet packet) {
    return 0;
  }

  @Override
  public void complete() {
    Runnable tmp;
    synchronized (this) {
      tmp = callback;
      callback = null;
    }
    if (tmp != null) {
      tmp.run();
    }
  }

  public int packLength(Packet packet, int len) {
    if (len < 256) {
      packet.put((byte) len);
    } else {
      packet.put((byte) 1);
      MQTTPacket.writeShort(packet, len + 2);
    }
    return len;
  }
}
