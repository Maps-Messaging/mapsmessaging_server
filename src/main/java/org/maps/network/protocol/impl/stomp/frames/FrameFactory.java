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

package org.maps.network.protocol.impl.stomp.frames;

import java.util.ArrayList;
import java.util.List;
import org.maps.network.io.Packet;
import org.maps.network.protocol.EndOfBufferException;
import org.maps.network.protocol.impl.stomp.StompProtocolException;
import org.maps.network.protocol.impl.stomp.listener.AbortListener;
import org.maps.network.protocol.impl.stomp.listener.AckListener;
import org.maps.network.protocol.impl.stomp.listener.BeginListener;
import org.maps.network.protocol.impl.stomp.listener.ClientHeartBeatListener;
import org.maps.network.protocol.impl.stomp.listener.CommitListener;
import org.maps.network.protocol.impl.stomp.listener.ConnectListener;
import org.maps.network.protocol.impl.stomp.listener.ConnectedListener;
import org.maps.network.protocol.impl.stomp.listener.DisconnectListener;
import org.maps.network.protocol.impl.stomp.listener.ErrorListener;
import org.maps.network.protocol.impl.stomp.listener.MessageListener;
import org.maps.network.protocol.impl.stomp.listener.NackListener;
import org.maps.network.protocol.impl.stomp.listener.ReceiptListener;
import org.maps.network.protocol.impl.stomp.listener.SendListener;
import org.maps.network.protocol.impl.stomp.listener.SubscribeListener;
import org.maps.network.protocol.impl.stomp.listener.UnsubscribeListener;

public class FrameFactory {

  private final List<FrameLookup> frames;
  private final byte[] workingBuffer;

  public FrameFactory(int maxBufferSize, boolean isClient) {
    frames = new ArrayList<>();
    if(isClient){
      frames.add(new FrameLookup("CONNECTED".getBytes(), new Connected(), new ConnectedListener()));
      frames.add(new FrameLookup("ERROR".getBytes(), new Error(), new ErrorListener()));
      frames.add(new FrameLookup("MESSAGE".getBytes(), new Message(maxBufferSize), new MessageListener()));
      frames.add(new FrameLookup("RECEIPT".getBytes(), new Receipt(), new ReceiptListener()));
    }
    else {
      frames.add(new FrameLookup("".getBytes(), new ClientHeartBeat(), new ClientHeartBeatListener()));
      frames.add(new FrameLookup("ABORT".getBytes(), new Abort(), new AbortListener()));
      frames.add(new FrameLookup("ACK".getBytes(), new Ack(), new AckListener()));
      frames.add(new FrameLookup("BEGIN".getBytes(), new Begin(), new BeginListener()));
      frames.add(new FrameLookup("CONNECT".getBytes(), new Connect(), new ConnectListener()));
      frames.add(new FrameLookup("STOMP".getBytes(), new Connect(), new ConnectListener()));
      frames.add(new FrameLookup("COMMIT".getBytes(), new Commit(), new CommitListener()));
      frames.add(new FrameLookup("DISCONNECT".getBytes(), new Disconnect(), new DisconnectListener()));
      frames.add(new FrameLookup("NACK".getBytes(), new Nack(), new NackListener()));
      frames.add(new FrameLookup("SEND".getBytes(), new Send(maxBufferSize), new SendListener()));
      frames.add(new FrameLookup("SUBSCRIBE".getBytes(), new Subscribe(), new SubscribeListener()));
      frames.add(new FrameLookup("UNSUBSCRIBE".getBytes(), new Unsubscribe(), new UnsubscribeListener()));
    }

    //
    // OK lets parse the verbs and find the longest, this restricts our search to this max length,
    // if we haven't found
    // a match then there isn't one to find so we can simply abort
    //
    int len = 0;
    for (FrameLookup lookup : frames) {
      len = Math.max(len, lookup.getCommand().length);
    }
    workingBuffer = new byte[len + 1];
  }

  public Frame parseFrame(Packet packet) throws StompProtocolException, EndOfBufferException {
    FrameLookup clientFrameLookup = createFrame(packet);
    if (clientFrameLookup == null) {
      throw new StompProtocolException("Unexpected Stomp frame received");
    }
    Frame frame = clientFrameLookup.getClientFrame().instance();
    frame.setListener(clientFrameLookup.getFrameListener());
    return frame;
  }

  private FrameLookup createFrame(Packet packet)
      throws StompProtocolException, EndOfBufferException {
    int pos = packet.position();
    int idx = parseForVerb(packet, pos);

    //
    // If we have exceeded the size of the command then we have corrupted the stream or its just
    // wrong
    //
    if (idx == workingBuffer.length) {
      packet.position(pos);
      throw new StompProtocolException("No known frame found::" + new String(workingBuffer));
    }

    //
    // OK we don't have enough data so lets get some more
    //
    if (idx == -1) {
      packet.position(pos);
      throw new EndOfBufferException();
    }

    //
    // OK we now have a command lets find the frame
    //
    for (FrameLookup lookup : frames) {
      byte[] command = lookup.getCommand();
      if (command.length == idx) {
        boolean found = true;
        for (int x = 0; x < command.length; x++) {
          if (command[x] != workingBuffer[x]) {
            found = false;
            break;
          }
        }
        if (found) {
          return lookup;
        }
      }
    }
    packet.position(pos);
    return null;
  }

  //
  // Scan the packet for a 0xA, indicating an end of verb OR
  // the max verb length OR
  // the end of the buffer
  //
  // If we hit a 0xA the chances are the packet is well formed, else, if we don't have enough
  // data in the packet we will need to do more reads until we do
  //
  // If we hit the end of the working buffer length and no 0xA, this means we have a corrupted frame
  // and can not really continue with this session, best log it and raise an exception to be handled
  //
  private int parseForVerb(Packet packet, int start) {
    int pos = start;
    int idx = 0;
    int end = packet.limit();
    while (pos < end && idx < workingBuffer.length) {
      workingBuffer[idx] = packet.get();
      pos++;
      if (workingBuffer[idx] == 0xA) {
        return idx;
      } else {
        idx++;
      }
    }
    if (idx == workingBuffer.length) {
      return idx;
    }
    return -1;
  }
}
