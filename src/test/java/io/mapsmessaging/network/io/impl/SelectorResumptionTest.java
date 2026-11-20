/*
 *
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

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.nio.channels.SelectionKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SelectorResumptionTest {

  @Test
  void selectorExecutesQueuedContinuationOnItsThread() throws Exception {
    Selector selector = new Selector();
    Thread thread = new Thread(selector);
    CountDownLatch executed = new CountDownLatch(1);
    AtomicReference<Thread> callbackThread = new AtomicReference<>();
    try {
      thread.start();
      selector.execute(() -> {
        callbackThread.set(Thread.currentThread());
        executed.countDown();
      });
      assertTrue(executed.await(5, TimeUnit.SECONDS));
      assertSame(thread, callbackThread.get());
    } finally {
      selector.close();
      thread.join(5000);
      assertFalse(thread.isAlive());
      selector.channelSelector.close();
    }
  }

  @Test
  void resumesBufferedBytesBeforeRestoringReadInterest() throws Exception {
    SelectorCallback callback = mock(SelectorCallback.class);
    EndPoint endpoint = mock(EndPoint.class);
    when(callback.getEndPoint()).thenReturn(endpoint);
    when(endpoint.getRemoteSocketAddress()).thenReturn("127.0.0.1:1234");
    EndPointConfigDTO config = mock(EndPointConfigDTO.class);
    when(config.getServerReadBufferSize()).thenReturn(1024L);
    when(config.getServerWriteBufferSize()).thenReturn(1024L);
    SelectorTask task = new SelectorTask(callback, config);
    when(callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
      Packet received = invocation.getArgument(0);
      assertEquals(2, received.available());
      received.position(received.limit());
      return true;
    });

    task.register(SelectionKey.OP_READ);
    task.suspendReads();
    Packet bytes = new Packet(8, false);
    bytes.put(new byte[]{1, 2});
    bytes.flip();
    task.getReadTask().pushOutstandingData(bytes);

    AtomicReference<Runnable> continuation = new AtomicReference<>();
    doAnswer(invocation -> {
      continuation.set(invocation.getArgument(0));
      return null;
    }).when(endpoint).executeOnSelector(any(Runnable.class));
    task.resumeReadsOnSelector();
    verify(callback, never()).processPacket(any(Packet.class));
    assertNotNull(continuation.get());

    continuation.get().run();
    verify(callback).processPacket(any(Packet.class));
    verify(endpoint, times(2)).register(eq(SelectionKey.OP_READ), same(task));
  }

  @Test
  void establishmentTransitionExecutesBeforeBufferedPacketReplay() throws Exception {
    SelectorCallback callback = mock(SelectorCallback.class);
    EndPoint endpoint = mock(EndPoint.class);
    when(callback.getEndPoint()).thenReturn(endpoint);
    EndPointConfigDTO config = mock(EndPointConfigDTO.class);
    when(config.getServerReadBufferSize()).thenReturn(1024L);
    when(config.getServerWriteBufferSize()).thenReturn(1024L);
    SelectorTask task = new SelectorTask(callback, config);
    java.util.concurrent.atomic.AtomicBoolean established = new java.util.concurrent.atomic.AtomicBoolean();
    when(callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
      assertTrue(established.get(), "Establishment must transition on the selector before replay");
      Packet packet = invocation.getArgument(0);
      packet.position(packet.limit());
      return true;
    });
    task.register(SelectionKey.OP_READ);
    task.suspendReads();
    Packet bytes = new Packet(8, false);
    bytes.put(new byte[]{1});
    bytes.flip();
    task.getReadTask().pushOutstandingData(bytes);

    AtomicReference<Runnable> continuation = new AtomicReference<>();
    doAnswer(invocation -> {
      continuation.set(invocation.getArgument(0));
      return null;
    }).when(endpoint).executeOnSelector(any(Runnable.class));
    task.resumeReadsOnSelector(() -> established.set(true));
    assertFalse(established.get());
    verify(callback, never()).processPacket(any(Packet.class));

    continuation.get().run();
    assertTrue(established.get());
    verify(callback).processPacket(any(Packet.class));
  }

  @Test
  void cancelledReadIsNotRestoredAfterReplay() throws Exception {
    SelectorCallback callback = mock(SelectorCallback.class);
    EndPoint endpoint = mock(EndPoint.class);
    when(callback.getEndPoint()).thenReturn(endpoint);
    when(endpoint.getRemoteSocketAddress()).thenReturn("127.0.0.1:1234");
    EndPointConfigDTO config = mock(EndPointConfigDTO.class);
    when(config.getServerReadBufferSize()).thenReturn(1024L);
    when(config.getServerWriteBufferSize()).thenReturn(1024L);
    SelectorTask task = new SelectorTask(callback, config);
    when(callback.processPacket(any(Packet.class))).thenAnswer(invocation -> {
      Packet received = invocation.getArgument(0);
      received.position(received.limit());
      task.cancel(SelectionKey.OP_READ);
      return true;
    });

    task.register(SelectionKey.OP_READ);
    task.suspendReads();
    Packet bytes = new Packet(8, false);
    bytes.put(new byte[]{3});
    bytes.flip();
    task.getReadTask().pushOutstandingData(bytes);

    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(0)).run();
      return null;
    }).when(endpoint).executeOnSelector(any(Runnable.class));
    task.resumeReadsOnSelector();
    verify(endpoint, times(1)).register(eq(SelectionKey.OP_READ), same(task));
  }
}
