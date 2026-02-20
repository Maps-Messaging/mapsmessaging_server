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

package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;


@Getter
@Setter
public class SatelliteMessage {
  private static final int HEADER_SIZE = 8;

  protected int streamNumber;
  protected boolean compressed;
  protected int packetNumber;
  protected int totalPackets;
  protected byte[] message;
  protected boolean raw;
  protected byte transformationId;

  @Setter
  private Runnable completionCallback;

  protected SatelliteMessage() {}


  public SatelliteMessage(int streamNumber, byte[] message, int packetNumber, int totalPackets, boolean compressed, byte transformationId) {
    this.streamNumber = streamNumber;
    this.message = message;
    this.compressed = compressed;
    this.packetNumber = packetNumber;
    this.totalPackets = totalPackets;
    this.transformationId = transformationId;
    raw = false;
  }

  public SatelliteMessage(byte[] incomingPackedMessage) {
    unpackFromReceived(incomingPackedMessage);
  }

  public byte[] packToSend() {
    ByteBuffer header = ByteBuffer.allocate( HEADER_SIZE + message.length);
    byte flag = compressed ? (byte) 0x1 : (byte) 0x0;
    byte transformed = (byte) (transformationId << 1);
    flag = (byte) (flag | transformed);
    header.put(flag);
    header.put((byte) (streamNumber & 0xff));
    header.putShort((short) packetNumber);
    header.putShort((short) totalPackets);
    header.putShort((short) message.length);
    header.put(message);
    return header.array();
  }

  protected void unpackFromReceived(byte[] data) {
    if (data == null) {
      return;
    }
    if(data.length < HEADER_SIZE) {
      message = data;
      raw = true;
      return;
    }

    ByteBuffer buffer = ByteBuffer.wrap(data);

    byte flag = buffer.get();
    compressed = (flag & 0b1) != 0;
    transformationId = (byte)((flag & 0xFE) >>> 1);
    streamNumber =  Byte.toUnsignedInt(buffer.get());
    packetNumber = Short.toUnsignedInt(buffer.getShort());
    totalPackets = Short.toUnsignedInt(buffer.getShort());
    int messageLength = Short.toUnsignedInt(buffer.getShort());

    if (buffer.remaining() < messageLength) {
      message = data;
      raw = true;
      return;
    }

    message = new byte[messageLength];
    buffer.get(message);
    raw = false;
  }


}
