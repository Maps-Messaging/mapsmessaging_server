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

package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

@Getter
public class SatelliteMessage {
  protected final int streamNumber;
  protected boolean compressed;
  protected int packetNumber;
  protected byte[] message;
  protected boolean raw;
  protected byte transformationId;

  @Setter
  private Runnable completionCallback;

  public SatelliteMessage(int streamNumber, byte[] message, int packetNumber, boolean compressed, byte transformationId) {
    this.streamNumber = streamNumber;
    this.message = message;
    this.compressed = compressed;
    this.packetNumber = packetNumber;
    this.transformationId = transformationId;
  }

  public SatelliteMessage(int streamNumber, byte[] incomingPackedMessage) {
    this.streamNumber = streamNumber;
    unpackFromReceived(incomingPackedMessage);
  }

  public byte[] packToSend() {
    ByteBuffer header = ByteBuffer.allocate( 7 + message.length);
    byte flag =compressed ? (byte) 0x1 : (byte) 0x0;
    flag = (byte)(( flag | (transformationId<<1))& 0xff);
    header.put(flag);
    header.putShort((short) packetNumber);
    header.putShort((short) message.length);
    header.put(message);
    return header.array();
  }

  protected void unpackFromReceived(byte[] data) {
    if (data == null) return;
    ByteBuffer buffer = ByteBuffer.wrap(data);
    //Load the flags, currently just compressed
    byte flag = buffer.get();
    compressed = (flag & 0b1) != 0;
    transformationId = (byte)(flag >>1);
    packetNumber = buffer.getShort();
    int messageLength = buffer.getShort();

    // Simple validate here
    if(buffer.remaining() < messageLength) {
      message = data;
      raw = true;
    }
    else {
      message = new byte[messageLength];
      buffer.get(message);
      raw = false;
    }
  }

}
