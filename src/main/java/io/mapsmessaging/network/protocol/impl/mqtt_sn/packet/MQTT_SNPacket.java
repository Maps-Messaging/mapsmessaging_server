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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import java.net.SocketAddress;

// http://www.mqtt.org/new/wp-content/uploads/2009/06/MQTT-SN_spec_v1.2.pdf
@java.lang.SuppressWarnings("squid:S00101")
public class MQTT_SNPacket implements ServerPacket {

  public static final int TOPIC_NAME = 0;
  public static final int PRE_DEFINED_ID = 1;
  public static final int SHORT_NAME = 2;

  public static final short ACCEPTED = 0;
  public static final short CONGESTION = 1;
  public static final short INVALID_TOPIC = 2;
  public static final short NOT_SUPPORTED = 3;

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
  protected int id;
  protected byte flags;
  private SocketAddress fromAddress;
  private Runnable completionHandler;

  public MQTT_SNPacket(int id) {
    this.id = id;
  }

  @Override
  public int packFrame(Packet packet) {
    return 0;
  }

  @Override
  public void complete() {
    Runnable tmp;
    synchronized (this) {
      tmp = completionHandler;
      completionHandler = null;
    }
    if (tmp != null) {
      tmp.run();
    }
  }

  public boolean dup() {
    return (flags & 0b10000000) != 0;
  }

  public QualityOfService getQoS() {
    return QualityOfService.getInstance((flags & 0b01100000) >> 5);
  }

  public void setQoS(QualityOfService qos) {
    flags = (byte) (flags | (byte) ((qos.getLevel() & 0b11) << 5));
  }

  public boolean retain() {
    return (flags & 0b00010000) != 0;
  }

  public boolean will() {
    return (flags & 0b00001000) != 0;
  }

  public boolean clean() {
    return (flags & 0b00000100) != 0;
  }

  public int topicIdType() {
    return (flags & 0b00000011);
  }

  public void setTopicIdType(int type) {
    flags = (byte) (flags | (type & 0b11));
  }

  public Runnable getCallback() {
    return completionHandler;
  }

  public void setCallback(Runnable completion) {
    completionHandler = completion;
  }

  public int getControlPacketId() {
    return id;
  }

  public SocketAddress getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(SocketAddress fromAddress) {
    this.fromAddress = fromAddress;
  }

  @Override
  public String toString() {
    return "Qos:" + getQoS() + " Retain:" + retain() + " Duplicate:" + dup() + " Will:" + will() + " Clean:" + clean() + " TopicIdType:" + topicIdType();
  }
}
