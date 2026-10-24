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

package io.mapsmessaging.network.io.impl.memory;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.ShmConfigDTO;
import io.mapsmessaging.memory.MemoryConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.Selector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MemoryEndPointTest {
  @Test
  void packet_io_preserves_partial_progress_and_statistics() throws Exception {
    MemoryConnection connection = mock(MemoryConnection.class);
    when(connection.remoteAddress()).thenReturn("peer");
    when(connection.write(any())).thenAnswer(invocation -> {
      ByteBuffer source = invocation.getArgument(0);
      source.position(source.position() + 2);
      return 2;
    });
    when(connection.read(any())).thenAnswer(invocation -> {
      ByteBuffer target = invocation.getArgument(0);
      target.put((byte) 42);
      return 1;
    });
    Selector selector = new Selector();
    try (MemoryEndPoint endpoint = new MemoryEndPoint(1, connection, selector, status(), List.of(), 1000)) {
      assertEquals(EndPoint.class, MemoryEndPoint.class.getSuperclass());
      Packet outgoing = new Packet(8, false);
      outgoing.put(new byte[] {1, 2, 3, 4});
      outgoing.flip();
      assertEquals(2, endpoint.sendPacket(outgoing));
      assertEquals(2, outgoing.position());
      Packet incoming = new Packet(8, false);
      assertEquals(1, endpoint.readPacket(incoming));
      assertEquals(1, incoming.position());
      assertEquals(2, endpoint.getEndPointStatus().getWriteBytesTotal());
      assertEquals(1, endpoint.getEndPointStatus().getReadBytesTotal());
      assertEquals("realm", endpoint.getAuthenticationConfig());
    } finally {
      selector.close();
      selector.wakeup();
    }
    verify(connection, times(1)).close();
  }

  @Test
  void readiness_runs_on_existing_selector_and_cancellation_stops_callbacks() throws Exception {
    MemoryConnection connection = mock(MemoryConnection.class);
    when(connection.remoteAddress()).thenReturn("peer");
    when(connection.hasData()).thenReturn(true);
    when(connection.canWrite()).thenReturn(true);
    Selector selector = new Selector();
    Thread selectorThread = Thread.ofPlatform().name("existing-selector").start(selector);
    AtomicReference<Thread> callbackThread = new AtomicReference<>();
    AtomicInteger selectedOps = new AtomicInteger();
    CountDownLatch called = new CountDownLatch(1);
    try (MemoryEndPoint endpoint = new MemoryEndPoint(2, connection, selector, status(), List.of(), 1000)) {
      SelectionKey key = endpoint.register(SelectionKey.OP_READ | SelectionKey.OP_WRITE, (task, selectedSelector, ops) -> {
        callbackThread.set(Thread.currentThread());
        selectedOps.set(ops);
        try {
          endpoint.register(0, task);
        } catch (Exception failure) {
          throw new AssertionError(failure);
        }
        called.countDown();
      }).get(2, TimeUnit.SECONDS);
      assertTrue(called.await(2, TimeUnit.SECONDS));
      assertSame(selectorThread, callbackThread.get());
      assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, selectedOps.get());
      assertEquals(0, key.interestOps());
      key.cancel();
      assertFalse(key.isValid());
      assertThrows(ClosedChannelException.class, () -> endpoint.register(SelectionKey.OP_READ, null));
      endpoint.close();
      endpoint.close();
    } finally {
      selector.close();
      selector.wakeup();
      selectorThread.join(2000);
    }
    assertFalse(selectorThread.isAlive());
    verify(connection, times(1)).close();
  }

  @Test
  void closing_peer_closes_endpoint_without_waiting_for_payload() throws Exception {
    MemoryConnection connection = mock(MemoryConnection.class);
    when(connection.remoteAddress()).thenReturn("peer");
    Selector selector = new Selector();
    Thread selectorThread = Thread.ofPlatform().start(selector);
    CountDownLatch closed = new CountDownLatch(1);
    EndPointServerStatus status = status();
    doAnswer(invocation -> { closed.countDown(); return null; }).when(status).handleCloseEndPoint(any());
    try (MemoryEndPoint endpoint = new MemoryEndPoint(3, connection, selector, status, List.of(), 1000)) {
      when(connection.isClosed()).thenReturn(true);
      assertTrue(closed.await(2, TimeUnit.SECONDS));
      assertTrue(endpoint.isClosed());
      assertThrows(IOException.class, () -> endpoint.sendPacket(new Packet(8, false)));
    } finally {
      selector.close();
      selector.wakeup();
      selectorThread.join(2000);
    }
  }

  @Test
  void failing_close_handler_still_releases_connection_and_updates_counts() throws Exception {
    MemoryConnection connection = mock(MemoryConnection.class);
    when(connection.remoteAddress()).thenReturn("peer");
    Selector selector = new Selector();
    long disconnections = EndPoint.totalDisconnections.sum();
    try (MemoryEndPoint endpoint = new MemoryEndPoint(4, connection, selector, status(), List.of(), 1000)) {
      endpoint.setCloseHandler(() -> { throw new IOException("close failure"); });
      IOException failure = assertThrows(IOException.class, endpoint::close);
      assertEquals("close failure", failure.getMessage());
      assertTrue(endpoint.isClosed());
      assertEquals(disconnections + 1, EndPoint.totalDisconnections.sum());
      verify(connection).close();
    } finally {
      selector.close();
      selector.wakeup();
    }
  }

  @Test
  void readiness_survives_existing_selector_rebuild() throws Exception {
    MemoryConnection connection = mock(MemoryConnection.class);
    when(connection.remoteAddress()).thenReturn("peer");
    when(connection.hasData()).thenReturn(true);
    Selector selector = new Selector();
    Thread thread = Thread.ofPlatform().unstarted(selector);
    CountDownLatch selectedAgain = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    try (MemoryEndPoint endpoint = new MemoryEndPoint(5, connection, selector, status(), List.of(), 1000)) {
      SelectionKey key = endpoint.register(0, (task, current, operations) -> { }).get();
      // Rebuild between selection iterations, as Selector.run() does, rather than inside a callback.
      Method rebuild = Selector.class.getDeclaredMethod("rebuildSelector");
      rebuild.setAccessible(true);
      rebuild.invoke(selector);
      assertTrue(key.isValid(), "Logical registration must survive physical selector migration");
      thread.start();
      endpoint.register(SelectionKey.OP_READ, (task, current, operations) -> {
        try {
          endpoint.register(0, task);
        } catch (Exception exception) {
          failure.set(exception);
        }
        selectedAgain.countDown();
      }).get();
      assertTrue(selectedAgain.await(2, TimeUnit.SECONDS));
      assertNull(failure.get());
    } finally {
      selector.close();
      selector.wakeup();
      thread.join(2000);
    }
  }

  @Test
  void zero_progress_writes_back_off_before_retrying() throws Exception {
    MemoryConnection connection = mock(MemoryConnection.class);
    when(connection.remoteAddress()).thenReturn("peer");
    when(connection.canWrite()).thenReturn(true);
    Selector selector = new Selector();
    Thread thread = Thread.ofPlatform().start(selector);
    AtomicInteger attempts = new AtomicInteger();
    CountDownLatch attempted = new CountDownLatch(3);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    long started = System.nanoTime();
    try (MemoryEndPoint endpoint = new MemoryEndPoint(6, connection, selector, status(), List.of(), 50_000)) {
      started = System.nanoTime();
      endpoint.register(SelectionKey.OP_WRITE, (task, current, operations) -> {
        try {
          assertEquals(0, endpoint.sendPacket(new Packet(8, false)));
          if (attempts.incrementAndGet() == 3) {
            endpoint.register(0, task);
          } else {
            endpoint.register(SelectionKey.OP_WRITE, task);
          }
        } catch (Exception exception) {
          failure.set(exception);
        }
        attempted.countDown();
      });
      assertTrue(attempted.await(2, TimeUnit.SECONDS));
      assertNull(failure.get());
      assertTrue(System.nanoTime() - started >= TimeUnit.MILLISECONDS.toNanos(100), "Zero progress must not immediately reschedule another write");
    } finally {
      selector.close();
      selector.wakeup();
      thread.join(2000);
    }
  }

  private static EndPointServerStatus status() {
    EndPointServerStatus status = mock(EndPointServerStatus.class);
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    config.setEndPointConfig(new ShmConfigDTO());
    config.setAuthenticationRealm("realm");
    when(status.getConfig()).thenReturn(config);
    return status;
  }
}
