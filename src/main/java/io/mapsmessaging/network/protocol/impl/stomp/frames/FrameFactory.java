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
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import io.mapsmessaging.network.protocol.impl.stomp.listener.*;

import java.util.ArrayList;
import java.util.List;

public class FrameFactory {

  private final List<FrameLookup> frames;
  private final byte[] workingBuffer;
  private boolean headerEscaping;

  public FrameFactory(int maxBufferSize, boolean isClient, boolean base64Encode) {
    frames = new ArrayList<>();
    if (isClient) {
      frames.add(new FrameLookup("CONNECTED".getBytes(), new Connected(), new ConnectedListener()));
      frames.add(new FrameLookup("ERROR".getBytes(), new Error(), new ErrorListener()));
      frames.add(new FrameLookup("MESSAGE".getBytes(), new Message(maxBufferSize, base64Encode), new MessageListener()));
      frames.add(new FrameLookup("RECEIPT".getBytes(), new Receipt(), new ReceiptListener()));
    } else {
      frames.add(new FrameLookup("".getBytes(), new ClientHeartBeat(), new ClientHeartBeatListener()));
      frames.add(new FrameLookup("ABORT".getBytes(), new Abort(), new AbortListener()));
      frames.add(new FrameLookup("ACK".getBytes(), new Ack(), new AckListener()));
      frames.add(new FrameLookup("BEGIN".getBytes(), new Begin(), new BeginListener()));
      frames.add(new FrameLookup("CONNECT".getBytes(), new Connect(), new ConnectListener()));
      frames.add(new FrameLookup("STOMP".getBytes(), new Connect(), new ConnectListener()));
      frames.add(new FrameLookup("COMMIT".getBytes(), new Commit(), new CommitListener()));
      frames.add(new FrameLookup("DISCONNECT".getBytes(), new Disconnect(), new DisconnectListener()));
      frames.add(new FrameLookup("NACK".getBytes(), new Nack(), new NackListener()));
      frames.add(new FrameLookup("SEND".getBytes(), new Send(maxBufferSize, base64Encode), new SendListener()));
      frames.add(new FrameLookup("SUBSCRIBE".getBytes(), new Subscribe(), new SubscribeListener()));
      frames.add(new FrameLookup("UNSUBSCRIBE".getBytes(), new Unsubscribe(), new UnsubscribeListener()));
    }

    int len = 0;
    for (FrameLookup lookup : frames) {
      len = Math.max(len, lookup.getCommand().length);
    }
    workingBuffer = new byte[len + 2];
    headerEscaping = true;
  }

  public void setHeaderEscaping(boolean headerEscaping) {
    this.headerEscaping = headerEscaping;
  }

  public Frame parseFrame(Packet packet) throws StompProtocolException, EndOfBufferException {
    FrameLookup clientFrameLookup = createFrame(packet);
    if (clientFrameLookup == null) {
      throw new StompProtocolException("Unexpected STOMP frame received");
    }
    Frame frame = clientFrameLookup.getClientFrame().instance();
    frame.setHeaderEscaping(headerEscaping);
    frame.setListener(clientFrameLookup.getFrameListener());
    return frame;
  }

  private FrameLookup createFrame(Packet packet)
      throws StompProtocolException, EndOfBufferException {
    int pos = packet.position();
    int commandLength = parseForVerb(packet);

    if (commandLength == Integer.MIN_VALUE) {
      packet.position(pos);
      throw new StompProtocolException("STOMP command exceeds the supported command length");
    }
    if (commandLength < 0) {
      packet.position(pos);
      throw new EndOfBufferException();
    }

    for (FrameLookup lookup : frames) {
      byte[] command = lookup.getCommand();
      if (command.length != commandLength) {
        continue;
      }
      boolean found = true;
      for (int index = 0; index < command.length; index++) {
        if (command[index] != workingBuffer[index]) {
          found = false;
          break;
        }
      }
      if (found) {
        return lookup;
      }
    }
    packet.position(pos);
    return null;
  }

  private int parseForVerb(Packet packet) throws StompProtocolException {
    int index = 0;
    while (packet.hasRemaining()) {
      byte value = packet.get();
      if (value == Frame.END_OF_LINE) {
        if (index > 0 && workingBuffer[index - 1] == Frame.CARRIAGE_RETURN) {
          index--;
        }
        return index;
      }
      if (value == Frame.END_OF_FRAME) {
        throw new StompProtocolException("NUL encountered in STOMP command line");
      }
      if (index >= workingBuffer.length) {
        return Integer.MIN_VALUE;
      }
      workingBuffer[index++] = value;
    }
    return -1;
  }
}
