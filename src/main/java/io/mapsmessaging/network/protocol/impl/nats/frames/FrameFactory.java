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

package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import io.mapsmessaging.network.protocol.impl.nats.listener.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FrameFactory {

  private final List<FrameLookup> frames;
  private final byte[] workingBuffer;

  public FrameFactory(int maxBufferSize, boolean isClient) {
    frames = new ArrayList<>();
    // In NATS server mode (you are the server)
    frames.add(new FrameLookup("CONNECT".getBytes(), new ConnectFrame(), new ConnectListener()));
    frames.add(new FrameLookup("PING".getBytes(), new PingFrame(), new PingListener()));
    frames.add(new FrameLookup("PONG".getBytes(), new PongFrame(), new PongListener()));
    frames.add(new FrameLookup("SUB".getBytes(), new SubFrame(), new SubListener()));
    frames.add(new FrameLookup("UNSUB".getBytes(), new UnsubFrame(), new UnsubListener()));
    frames.add(new FrameLookup("PUB".getBytes(), new PubFrame(maxBufferSize), new PubListener()));
    frames.add(new FrameLookup("HPUB".getBytes(), new HPubFrame(maxBufferSize), new PubListener()));
    frames.add(new FrameLookup("MSG".getBytes(), new MsgFrame(maxBufferSize), new MsgListener()));
    frames.add(new FrameLookup("HMSG".getBytes(), new HMsgFrame(maxBufferSize), new MsgListener()));
    frames.add(new FrameLookup("INFO".getBytes(), new InfoFrame(maxBufferSize), new InfoListener()));
    frames.add(new FrameLookup("+OK".getBytes(), new OkFrame(), new OkListener()));
    frames.add(new FrameLookup("-ERR".getBytes(), new ErrFrame(), new ErrListener()));

    int len = 0;
    for (FrameLookup lookup : frames) {
      len = Math.max(len, lookup.getCommand().length);
    }
    workingBuffer = new byte[len + 1];
  }

  public NatsFrame parseFrame(Packet packet) throws NatsProtocolException, EndOfBufferException {
    FrameLookup clientFrameLookup = createFrame(packet);
    if (clientFrameLookup == null) {
      throw new NatsProtocolException("Unexpected NATS frame received");
    }
    NatsFrame frame = clientFrameLookup.getFrame().instance();
    frame.setListener(clientFrameLookup.getFrameListener());
    return frame;
  }

  private FrameLookup createFrame(Packet packet) throws NatsProtocolException, EndOfBufferException {
    int pos = packet.position();
    int idx = parseForVerb(packet, pos);

    if (idx == workingBuffer.length) {
      packet.position(pos);
      throw new NatsProtocolException("No known NATS frame found::" + new String(workingBuffer));
    }

    if (idx == -1) {
      packet.position(pos);
      throw new EndOfBufferException();
    }

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

  private int parseForVerb(Packet packet, int start) {
    int pos = start;
    int idx = 0;
    int end = packet.limit();
    Arrays.fill(workingBuffer, (byte) 0);
    while (pos < end && idx < workingBuffer.length) {
      byte b = packet.get();
      workingBuffer[idx++] = b;
      pos++;

      if (b == ' ') {
        return idx - 1; // Position of the last letter before space
      } else if (b == '\r') {
        if (pos < end) {
          byte next = packet.get();
          if (next == '\n') {
            return idx - 1; // Position up to \r
          } else {
            throw new IllegalStateException("Invalid frame: \\r not followed by \\n");
          }
        } else {
          // Not enough data yet
          return -1;
        }
      }
    }
    if (idx == workingBuffer.length) {
      return idx;
    }
    return -1;
  }
}
