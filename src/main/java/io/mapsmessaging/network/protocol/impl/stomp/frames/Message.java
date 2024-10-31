/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPublishPacket;
import java.nio.ByteBuffer;

public class Message extends Event implements ServerPublishPacket {

  private static final byte[] COMMAND = "MESSAGE".getBytes();

  public Message(int maxBufferSize) {
    super(maxBufferSize);
  }

  @Override
  public Frame instance() {
    return new Message(maxBufferSize);
  }

  byte[] getCommand() {
    return COMMAND;
  }

  public void packMessage(String destination, String subscriptionId, io.mapsmessaging.api.message.Message internalMessage) {
    super.packMessage(destination, internalMessage);

    // This is only present in MESSAGE
    putHeader("subscription", subscriptionId);
    putHeader("message-id", "" + internalMessage.getIdentifier());
    putHeader("priority", "" + internalMessage.getPriority());
  }

  @Override
  public String toString() {
    return "STOMP Message[ Header:" + getHeaderAsString() + "]";
  }

  @Override
  public Packet[] packAdvancedFrame(Packet packet) {
    packHeader(packet);
    if(this.getData().length < packet.available()) {
      packet.put(this.getData());
      packet.put((byte) 0x0);
      return new Packet[]{packet};
    }
    Packet payloadPacket = new Packet(ByteBuffer.wrap(this.getData()));
    ByteBuffer endOfFrame = ByteBuffer.allocate(1);
    endOfFrame.put((byte)0);
    endOfFrame.flip();
    return new Packet[]{packet, payloadPacket, new Packet(endOfFrame) };
  }

  @Override
  public void packBody(Packet packet) {
    // requires the extending class to provide this mechanism, if one is required
  }

}
