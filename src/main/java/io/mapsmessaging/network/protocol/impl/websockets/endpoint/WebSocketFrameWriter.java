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

package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import io.mapsmessaging.network.io.Packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

final class WebSocketFrameWriter {

  private final PacketSink sink;
  private final Deque<PendingFrame> controlFrames = new ArrayDeque<>();
  private final Deque<PendingFrame> dataFrames = new ArrayDeque<>();
  private final Map<Packet, PendingFrame> sourceFrames = new IdentityHashMap<>();

  private PendingFrame currentFrame;
  private int lastNetworkBytesWritten;

  WebSocketFrameWriter(PacketSink sink) {
    this.sink = sink;
  }

  synchronized int writeBinary(Packet payload) throws IOException {
    lastNetworkBytesWritten = 0;
    PendingFrame frame = sourceFrames.get(payload);
    if (frame == null) {
      frame = PendingFrame.forPayload(WebSocketFrameDecoder.BINARY, payload);
      sourceFrames.put(payload, frame);
      dataFrames.offer(frame);
      markSourcePending(frame);
    }

    flush();
    if (frame.complete) {
      sourceFrames.remove(payload);
      payload.limit(frame.sourceEnd);
      payload.position(frame.sourceEnd);
      return frame.payloadLength;
    }
    return 0;
  }

  synchronized boolean writeControl(int opcode, byte[] payload) throws IOException {
    if (opcode != WebSocketFrameDecoder.PONG && opcode != WebSocketFrameDecoder.CLOSE) {
      throw new IOException("Unsupported server WebSocket control opcode " + opcode);
    }
    if (payload.length > 125) {
      throw new IOException("WebSocket control payload exceeds 125 bytes");
    }

    lastNetworkBytesWritten = 0;
    PendingFrame frame = PendingFrame.forControl(opcode, payload);
    controlFrames.offer(frame);
    flush();
    return frame.complete;
  }

  synchronized int getLastNetworkBytesWritten() {
    return lastNetworkBytesWritten;
  }

  synchronized boolean hasPendingApplicationData() {
    return (currentFrame != null && currentFrame.source != null) || !dataFrames.isEmpty();
  }

  private void flush() throws IOException {
    while (true) {
      if (currentFrame == null) {
        currentFrame = controlFrames.poll();
        if (currentFrame == null) {
          currentFrame = dataFrames.poll();
        }
        if (currentFrame == null) {
          return;
        }
      }

      int written = sink.write(currentFrame.encoded);
      if (written < 0) {
        throw new IOException("WebSocket transport closed during write");
      }
      lastNetworkBytesWritten += written;
      if (currentFrame.encoded.hasRemaining()) {
        if (written == 0) {
          return;
        }
        continue;
      }

      currentFrame.complete = true;
      currentFrame = null;
    }
  }

  private void markSourcePending(PendingFrame frame) {
    if (frame.sourceStart != 0) {
      return;
    }
    if (frame.payloadLength > 1) {
      frame.source.position(1);
    } else if (frame.payloadLength == 1 && frame.source.capacity() > frame.sourceEnd) {
      frame.source.limit(frame.sourceEnd + 1);
      frame.source.position(frame.sourceEnd);
    }
  }

  private static Packet encode(int opcode, byte[] payload) {
    int headerLength = payload.length < 126 ? 2 : payload.length <= 0xFFFF ? 4 : 10;
    Packet encoded = new Packet(headerLength + payload.length, false);
    encoded.put((byte) (0x80 | opcode));
    if (payload.length < 126) {
      encoded.put((byte) payload.length);
    } else if (payload.length <= 0xFFFF) {
      encoded.put((byte) 126);
      encoded.put((byte) ((payload.length >>> 8) & 0xFF));
      encoded.put((byte) (payload.length & 0xFF));
    } else {
      encoded.put((byte) 127);
      long length = payload.length;
      for (int shift = 56; shift >= 0; shift -= 8) {
        encoded.put((byte) ((length >>> shift) & 0xFF));
      }
    }
    encoded.put(payload);
    encoded.flip();
    return encoded;
  }

  private static final class PendingFrame {
    private final Packet encoded;
    private final Packet source;
    private final int sourceStart;
    private final int sourceEnd;
    private final int payloadLength;
    private boolean complete;

    private PendingFrame(Packet encoded, Packet source, int sourceStart, int sourceEnd, int payloadLength) {
      this.encoded = encoded;
      this.source = source;
      this.sourceStart = sourceStart;
      this.sourceEnd = sourceEnd;
      this.payloadLength = payloadLength;
    }

    private static PendingFrame forPayload(int opcode, Packet source) {
      int start = source.position();
      int end = source.limit();
      byte[] payload = new byte[end - start];
      ByteBuffer duplicate = source.getRawBuffer().duplicate();
      duplicate.position(start);
      duplicate.limit(end);
      duplicate.get(payload);
      return new PendingFrame(encode(opcode, payload), source, start, end, payload.length);
    }

    private static PendingFrame forControl(int opcode, byte[] payload) {
      return new PendingFrame(encode(opcode, payload.clone()), null, 0, 0, payload.length);
    }
  }

  @FunctionalInterface
  interface PacketSink {
    int write(Packet packet) throws IOException;
  }
}
