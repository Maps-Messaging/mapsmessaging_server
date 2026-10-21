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

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPublishPacket;

import java.nio.ByteBuffer;

public class Message extends Event implements ServerPublishPacket {

  private static final byte[] COMMAND = "MESSAGE".getBytes();

  public Message(int maxBufferSize, boolean base64Encode) {
    super(maxBufferSize, base64Encode);
  }

  @Override
  public Frame instance() {
    return new Message(maxBufferSize, base64Encode);
  }

  byte[] getCommand() {
    return COMMAND;
  }

  public void packMessage(
      String destination,
      String subscriptionId,
      io.mapsmessaging.api.message.Message internalMessage) {
    packMessage(destination, subscriptionId, internalMessage, false, false);
  }

  public void packMessage(
      String destination,
      String subscriptionId,
      io.mapsmessaging.api.message.Message internalMessage,
      boolean stomp12,
      boolean acknowledgementRequired) {
    super.packMessage(destination, internalMessage);

    long identifier = internalMessage.getIdentifier();
    putHeader("subscription", subscriptionId);
    putHeader("message-id", Long.toString(identifier));
    putHeader("priority", Integer.toString(internalMessage.getPriority().getValue()));
    if (stomp12 && acknowledgementRequired) {
      putHeader("ack", AcknowledgementToken.create(subscriptionId, identifier));
    }
  }

  @Override
  public String toString() {
    return "STOMP Message[ Header:" + getHeaderAsString() + "]";
  }

  @Override
  public Packet[] packAdvancedFrame(Packet packet) {
    packHeader(packet);
    if (getData().length < packet.available()) {
      packet.put(getData());
      packet.put(END_OF_FRAME);
      return new Packet[]{packet};
    }
    Packet payloadPacket = new Packet(ByteBuffer.wrap(getData()));
    ByteBuffer endOfFrame = ByteBuffer.allocate(1);
    endOfFrame.put(END_OF_FRAME);
    endOfFrame.flip();
    return new Packet[]{packet, payloadPacket, new Packet(endOfFrame)};
  }

  @Override
  public void packBody(Packet packet) {
    // ServerPublishPacket packs large payloads without copying them into the coalescing packet.
  }
}
