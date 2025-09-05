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

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.EndOfBufferException;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public abstract class MQTTPacket implements ServerPacket {

  // http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718020
  public static final int CONNECT = 1;
  public static final int CONNACK = 2;
  public static final int PUBLISH = 3;
  public static final int PUBACK = 4;
  public static final int PUBREC = 5;
  public static final int PUBREL = 6;
  public static final int PUBCOMP = 7;
  public static final int SUBSCRIBE = 8;
  public static final int SUBACK = 9;
  public static final int UNSUBSCRIBE = 10;
  public static final int UNSUBACK = 11;
  public static final int PINGREQ = 12;
  public static final int PINGRESP = 13;
  public static final int DISCONNECT = 14;

  private final int controlPacketId;
  private Runnable completionHandler;

  protected MQTTPacket(int id) {
    controlPacketId = id;
  }

  public static long readVariableInt(Packet packet)
      throws MalformedException, EndOfBufferException {
    long value = 0;
    byte encoded;
    int counter = 0;
    do {
      if (packet.available() == 0) {
        throw new EndOfBufferException("Ran off the buffer reading variable int");
      }
      encoded = packet.get();
      value += ((encoded & 0x7FL) << (7L * counter));
      if (counter > 4) {
        throw new MalformedException();
      }
      counter++;
    } while ((encoded & 0x80) != 0);
    return value;
  }

  public static void writeVariableInt(Packet packet, long val) {
    long temp = val;
    if (temp != 0) {
      while (temp != 0) {
        int part = (byte) temp & 0x7f;
        temp = temp >> 7;
        if (temp != 0) {
          part = part + 0x80;
        }
        packet.put((byte) part);
      }
    } else {
      packet.put((byte) 0);
    }
  }

  public static byte[] readRemaining(Packet packet, int maxLen) {
    byte[] tmp = new byte[maxLen];
    packet.get(tmp);
    return tmp;
  }

  public static String readRemainingString(Packet packet) throws MalformedException {
    String str = new String(readRemaining(packet, packet.available()), StandardCharsets.UTF_8);
    if (str.chars().anyMatch(c -> c == 0)) {
      throw new MalformedException("UTF-8 String must not contain U+0000 characters [MQTT-1.5.3-2]");
    }
    return str;
  }

  public static String readUTF8(Packet packet) throws MalformedException {
    String result = "";
    if (packet.available() >= 2) {
      int len = readShort(packet);
      if (packet.available() >= len) {
        byte[] str = new byte[len];
        packet.get(str, 0, len);
        result = new String(str, StandardCharsets.UTF_8);
        if (result.chars().anyMatch(c -> c == 0)) {
          throw new MalformedException("UTF-8 String must not contain U+0000 characters [MQTT-1.5.3-2]");
        }
      } else {
        throw new MalformedException("Packet truncated");
      }
    }
    return result;
  }

  public static void writeUTF8(Packet packet, String string) {
    byte[] data = string.getBytes();
    int len = data.length;
    packet.put((byte) ((len >> 8) & 0xff));
    packet.put((byte) ((len) & 0xff));
    packet.put(data);
  }

  public static byte[] readBuffer(Packet packet) {
    int len = readShort(packet);
    byte[] str = new byte[len];
    packet.get(str, 0, len);
    return str;
  }

  public static void writeBuffer(byte[] buffer, Packet packet) {
    writeShort(packet, buffer.length);
    packet.put(buffer);
  }

  public static int readShort(Packet packet) {
    return (packet.get() & 0xff) << 8 | (packet.get() & 0xff);
  }

  public static void writeShort(Packet packet, int value) {
    packet.put((byte) ((value >> 8) & 0xff));
    packet.put((byte) ((value) & 0xff));
  }

  public static long readInt(Packet packet) {
    long tmp = (packet.get() & 0xff);
    tmp = tmp << 8;
    tmp = tmp + (packet.get() & 0xff);
    tmp = tmp << 8;
    tmp = tmp + (packet.get() & 0xff);
    tmp = tmp << 8;
    tmp = tmp + (packet.get() & 0xff);
    return tmp;
  }

  public static void writeInt(Packet packet, long value) {
    packet.put((byte) ((value >> 24) & 0xff));
    packet.put((byte) ((value >> 16) & 0xff));
    packet.put((byte) ((value >> 8) & 0xff));
    packet.put((byte) ((value) & 0xff));
  }

  public static void writeRawBuffer(byte[] buffer, Packet packet) {
    packet.put(buffer);
  }

  protected void packControlByte(Packet packet, int reservedBits) {
    byte tmp = (byte) (controlPacketId << 4);
    tmp = (byte) (tmp | (reservedBits & 0xf));
    packet.put(tmp);
  }

  public int getControlPacketId() {
    return controlPacketId;
  }

  public Runnable getCallback() {
    return completionHandler;
  }

  public void setCallback(Runnable completion) {
    completionHandler = completion;
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

  public SocketAddress getFromAddress() {
    return null;
  }
}
