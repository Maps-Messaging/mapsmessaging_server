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

package io.mapsmessaging.network.io;

import lombok.Getter;
import lombok.Setter;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * This class wraps the Java ByteBuffer classes and manages the buffer.
 */
public class Packet {

  @Getter
  @Setter
  private static boolean ERASE_BUFFER = false;

  /**
   * underlying ByteBuffer to use @See  java.nio.ByteBuffer
   */
  private final ByteBuffer buffer;

  /**
   * SocketAddress is used for non stream based protocols, like UDP to set where this packet arrived from
   */
  @Getter
  @Setter
  private SocketAddress fromAddress;

  public Packet(int size, boolean direct) {
    if (direct) {
      buffer = ByteBuffer.allocateDirect(size);
    } else {
      buffer = ByteBuffer.allocate(size);
    }
  }

  public Packet(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  protected Packet(Packet packet) {
    buffer = packet.buffer;
    fromAddress = packet.fromAddress;
  }

  public int available() {
    return buffer.limit() - buffer.position();
  }

  public void flip() {
    buffer.flip();
  }

  public int limit() {
    return buffer.limit();
  }

  public void limit(int limit) {
    buffer.limit(limit);
  }

  public int position() {
    return buffer.position();
  }

  public Packet position(int pos) {
    buffer.position(pos);
    return this;
  }

  public int capacity() {
    return buffer.capacity();
  }

  public boolean hasData() {
    return buffer.position() != buffer.limit() && buffer.position() != 0;
  }

  public boolean hasRemaining() {
    return buffer.hasRemaining();
  }

  public Packet clear() {
    buffer.clear();
    if(ERASE_BUFFER){
      for (int x = 0; x < buffer.capacity(); x++) {
        buffer.put(x, (byte) 0);
      }
      buffer.clear();
    }
    return this;
  }

  public void compact() {
    if (buffer.position() == buffer.limit()) {
      clear();
    } else {
      buffer.compact();
    }
  }

  public byte get(int pos) {
    return buffer.get(pos);
  }

  public byte get() {
    return buffer.get();
  }

  public int getByte() {
    return buffer.get() & 0xff;
  }

  public void put(byte b) {
    buffer.put(b);
  }

  public void putByte(int b) {
    buffer.put((byte) (b & 0xff));
  }

  public void putShort(int b) {
    buffer.put((byte) ((b >> 8) & 0xff));
    buffer.put((byte) (b & 0xff));
  }

  public int getShort() {
    return (buffer.get() & 0xff) << 8 | (buffer.get() & 0xff);
  }


  public void put(int pos, byte b) {
    buffer.put(pos, b);
  }

  public Packet get(byte[] buf) {
    buffer.get(buf);
    return this;
  }

  public Packet put(byte[] buf) {
    buffer.put(buf);
    return this;
  }

  public Packet get(byte[] b, int offset, int len) {
    buffer.get(b, offset, len);
    return this;
  }

  public Packet put(byte[] b, int offset, int len) {
    buffer.put(b, offset, len);
    return this;
  }

  public Packet put(Packet packet) {
    buffer.put(packet.buffer);
    return this;
  }

  /*
   * Should only be used by protocols to access the physical buffer to pass to network layers or storage layers
   */
  public ByteBuffer getRawBuffer() {
    return buffer;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    int pos = buffer.position();
    int len = buffer.limit();
    int end = Math.min(len, pos + 200);
    if (fromAddress != null) {
      sb.append("From: ").append(fromAddress);
    }
    sb.append(" Position:").append(pos).append(" Len:").append(len).append("[");
    for (int x = pos; x < end; x++) {
      sb.append(String.format("%02X", (buffer.get(x)))).append(",");
    }
    sb.append("][");

    byte[] t = new byte[1];
    for (int x = pos; x < end; x++) {
      t[0] = buffer.get(x);
      if (t[0] < 32 || t[0] > 126) {
        t[0] = '#';
      }
      sb.append(new String(t));
    }

    sb.append("]");

    return sb.toString();
  }

}
