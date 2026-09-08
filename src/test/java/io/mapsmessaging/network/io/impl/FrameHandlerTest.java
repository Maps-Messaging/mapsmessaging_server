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

package io.mapsmessaging.network.io.impl;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.ServerPublishPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FrameHandlerTest {

  @Test
  void retainsCoalescedPacketAfterBlockedAndPartialWrites() throws Exception {
    TestPacket frame = new TestPacket(new byte[]{1, 2, 3, 4, 5, 6, 7});
    WriteFixture fixture = new WriteFixture(16, 2);

    fixture.writeTask.push(frame);
    fixture.writeTask.handleWrite();

    Assertions.assertArrayEquals(new byte[0], fixture.output.toByteArray());
    Assertions.assertEquals(0, frame.completed.get());

    fixture.drain();

    Assertions.assertArrayEquals(frame.data, fixture.output.toByteArray());
    Assertions.assertEquals(1, frame.completed.get());
  }

  @Test
  void retainsAdvancedPublishSegmentsInOrderUntilFullyWritten() throws Exception {
    byte[] header = new byte[]{10, 11, 12, 13};
    byte[] payload = new byte[]{20, 21, 22, 23, 24, 25, 26};
    SegmentedTestPacket frame = new SegmentedTestPacket(header, payload);
    WriteFixture fixture = new WriteFixture(8, 3);

    fixture.writeTask.push(frame);
    fixture.writeTask.handleWrite();

    Assertions.assertArrayEquals(new byte[0], fixture.output.toByteArray());
    Assertions.assertEquals(0, frame.completed.get());

    fixture.drain();

    ByteArrayOutputStream expected = new ByteArrayOutputStream();
    expected.writeBytes(header);
    expected.writeBytes(payload);
    Assertions.assertArrayEquals(expected.toByteArray(), fixture.output.toByteArray());
    Assertions.assertEquals(1, frame.completed.get());
  }

  private static class WriteFixture {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final AtomicInteger writeAttempts = new AtomicInteger();
    private final WriteTask writeTask;

    private WriteFixture(int bufferSize, int maximumWriteSize) throws Exception {
      SelectorCallback callback = mock(SelectorCallback.class);
      SelectorTask selectorTask = mock(SelectorTask.class);
      Logger logger = mock(Logger.class);
      EndPoint endPoint = mock(EndPoint.class);
      when(callback.getEndPoint()).thenReturn(endPoint);
      when(endPoint.sendPacket(any(Packet.class))).thenAnswer(invocation -> {
        if (writeAttempts.getAndIncrement() == 0) {
          return 0;
        }
        Packet packet = invocation.getArgument(0);
        ByteBuffer buffer = packet.getRawBuffer();
        int length = Math.min(maximumWriteSize, buffer.remaining());
        byte[] data = new byte[length];
        buffer.get(data);
        output.writeBytes(data);
        return length;
      });
      writeTask = new WriteTask(callback, bufferSize, selectorTask, logger);
    }

    private void drain() {
      for (int count = 0; count < 20; count++) {
        writeTask.handleWrite();
      }
    }
  }

  private static class TestPacket implements ServerPacket {
    protected final byte[] data;
    protected final AtomicInteger completed = new AtomicInteger();

    private TestPacket(byte[] data) {
      this.data = data;
    }

    @Override
    public int packFrame(Packet packet) {
      packet.put(data);
      return data.length;
    }

    @Override
    public void complete() {
      completed.incrementAndGet();
    }

    @Override
    public SocketAddress getFromAddress() {
      return null;
    }
  }

  private static class SegmentedTestPacket extends TestPacket implements ServerPublishPacket {
    private final byte[] payload;

    private SegmentedTestPacket(byte[] header, byte[] payload) {
      super(header);
      this.payload = payload;
    }

    @Override
    public Packet[] packAdvancedFrame(Packet packet) {
      packet.put(data);
      return new Packet[]{packet, new Packet(ByteBuffer.wrap(payload))};
    }
  }
}
