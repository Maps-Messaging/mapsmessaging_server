/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.io.impl.canbus;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.protocol.Protocol;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CanbusEndPointServerLifecycleTest {

  @Test
  void construction_does_not_create_device_or_schedule_work() throws Exception {
    try (Fixture fixture = fixture(false)) {
      assertEquals(0, fixture.server.attempts);
      assertTrue(fixture.server.retries.isEmpty());
      assertEquals(0, fixture.server.size());
    }
  }

  @Test
  void start_creates_and_registers_endpoint_immediately() throws Exception {
    try (Fixture fixture = fixture(false)) {
      CanbusEndPoint endpoint = fixture.endpoint(1);
      fixture.server.outcomes.add(endpoint);
      fixture.server.start();
      assertEquals(List.of(endpoint), fixture.server.getActiveEndPoints());
      assertTrue(fixture.server.retries.isEmpty());
      fixture.server.start();
      assertEquals(1, fixture.server.attempts);
    }
  }

  @Test
  void missing_socketcan_device_retries_until_available() throws Exception {
    try (Fixture fixture = fixture(false)) {
      fixture.server.outcomes.add(new IOException("device missing"));
      fixture.server.start();
      assertEquals(0, fixture.server.size());
      assertEquals(1, fixture.server.retries.size());
      CanbusEndPoint endpoint = fixture.endpoint(2);
      fixture.server.outcomes.add(endpoint);
      fixture.server.retries.getFirst().run();
      assertEquals(List.of(endpoint), fixture.server.getActiveEndPoints());
      assertEquals(2, fixture.server.attempts);
      assertEquals(1, fixture.server.retries.size());
    }
  }

  @Test
  void stop_cancels_retry_and_late_task_cannot_create_device() throws Exception {
    try (Fixture fixture = fixture(false)) {
      fixture.server.outcomes.add(new IOException("device missing"));
      fixture.server.start();
      fixture.server.close();
      verify(fixture.server.futures.getFirst()).cancel(false);
      fixture.server.retries.getFirst().run();
      assertEquals(1, fixture.server.attempts);
      assertEquals(0, fixture.server.size());
    }
  }

  @Test
  void retry_from_previous_start_cannot_affect_restarted_server() throws Exception {
    try (Fixture fixture = fixture(false)) {
      fixture.server.outcomes.add(new IOException("device missing"));
      fixture.server.start();
      Runnable stale = fixture.server.retries.getFirst();
      fixture.server.close();
      CanbusEndPoint endpoint = fixture.endpoint(3);
      fixture.server.outcomes.add(endpoint);
      fixture.server.start();
      stale.run();
      assertEquals(2, fixture.server.attempts);
      assertEquals(List.of(endpoint), fixture.server.getActiveEndPoints());
    }
  }

  @Test
  void protocol_creation_failure_releases_partial_endpoint_before_retry() throws Exception {
    try (Fixture fixture = fixture(false)) {
      CanbusEndPoint endpoint = fixture.endpoint(4);
      fixture.server.outcomes.add(endpoint);
      fixture.server.bindingFailure = new IOException("protocol failed");
      fixture.server.start();
      verify(endpoint.getBoundProtocol()).close();
      verify(endpoint).close();
      assertEquals(0, fixture.server.size());
      assertEquals(1, fixture.server.retries.size());
    }
  }

  @Test
  void active_device_failure_cleans_up_and_retries_once() throws Exception {
    try (Fixture fixture = fixture(false)) {
      CanbusEndPoint endpoint = fixture.endpoint(5);
      fixture.server.outcomes.add(endpoint);
      fixture.server.start();
      fixture.server.handleCloseEndPoint(endpoint);
      fixture.server.handleCloseEndPoint(endpoint);
      verify(endpoint.getBoundProtocol()).close();
      verify(endpoint).close();
      assertEquals(0, fixture.server.size());
      assertEquals(1, fixture.server.retries.size());
    }
  }

  @Test
  void repeated_start_stop_creates_fresh_endpoints_and_closes_each_once() throws Exception {
    try (Fixture fixture = fixture(false)) {
      for (int cycle = 0; cycle < 3; cycle++) {
        CanbusEndPoint endpoint = fixture.endpoint(10 + cycle);
        fixture.server.outcomes.add(endpoint);
        fixture.server.start();
        assertEquals(List.of(endpoint), fixture.server.getActiveEndPoints());
        fixture.server.close();
        fixture.server.close();
        verify(endpoint.getBoundProtocol()).close();
        verify(endpoint).close();
        assertEquals(0, fixture.server.size());
      }
      assertTrue(fixture.server.retries.isEmpty());
    }
  }

  @Test
  void close_failure_still_releases_endpoint_and_bookkeeping() throws Exception {
    try (Fixture fixture = fixture(false)) {
      CanbusEndPoint endpoint = fixture.endpoint(20);
      fixture.server.outcomes.add(endpoint);
      fixture.server.start();
      Protocol protocol = endpoint.getBoundProtocol();
      doThrow(new IOException("protocol close failed")).when(protocol).close();
      assertThrows(IOException.class, fixture.server::close);
      verify(endpoint).close();
      assertEquals(0, fixture.server.size());
      assertTrue(fixture.server.retries.isEmpty());
    }
  }

  @Test
  void serial_can_failure_does_not_start_socketcan_retry_loop() throws Exception {
    try (Fixture fixture = fixture(true)) {
      fixture.server.outcomes.add(new IOException("serial unavailable"));
      fixture.server.start();
      assertEquals(1, fixture.server.attempts);
      assertTrue(fixture.server.retries.isEmpty());
    }
  }

  @Test
  void restart_waits_for_previous_cleanup_to_finish() throws Exception {
    try (Fixture fixture = fixture(false)) {
      CanbusEndPoint oldEndpoint = fixture.endpoint(30);
      CanbusEndPoint replacement = fixture.endpoint(31);
      fixture.server.outcomes.add(oldEndpoint);
      fixture.server.outcomes.add(replacement);
      fixture.server.start();
      java.util.concurrent.CountDownLatch closing = new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
      Protocol protocol = oldEndpoint.getBoundProtocol();
      doAnswer(call -> {
        closing.countDown();
        assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS));
        return null;
      }).when(protocol).close();
      java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
      try {
        java.util.concurrent.Future<?> stopped = executor.submit(() -> {
          try (MockedStatic<MessageDaemon> daemon = mockStatic(MessageDaemon.class)) {
            daemon.when(MessageDaemon::getInstance).thenReturn(mock(MessageDaemon.class, RETURNS_DEEP_STUBS));
            fixture.server.close();
          }
          return null;
        });
        assertTrue(closing.await(5, java.util.concurrent.TimeUnit.SECONDS));
        java.util.concurrent.CountDownLatch starting = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Future<?> restarted = executor.submit(() -> {
          try (MockedStatic<MessageDaemon> daemon = mockStatic(MessageDaemon.class)) {
            daemon.when(MessageDaemon::getInstance).thenReturn(mock(MessageDaemon.class, RETURNS_DEEP_STUBS));
            starting.countDown();
            fixture.server.start();
          }
          return null;
        });
        assertTrue(starting.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertThrows(java.util.concurrent.TimeoutException.class,
            () -> restarted.get(100, java.util.concurrent.TimeUnit.MILLISECONDS));
        release.countDown();
        stopped.get(5, java.util.concurrent.TimeUnit.SECONDS);
        restarted.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(List.of(replacement), fixture.server.getActiveEndPoints());
      } finally {
        release.countDown();
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
      }
    }
  }

  private static Fixture fixture(boolean serial) {
    MockedStatic<MessageDaemon> daemon = mockStatic(MessageDaemon.class);
    daemon.when(MessageDaemon::getInstance).thenReturn(mock(MessageDaemon.class, RETURNS_DEEP_STUBS));
    CanbusConfigDTO canbus = new CanbusConfigDTO();
    canbus.setDeviceName("vcan0");
    if (serial) {
      canbus.setSerialConfig(mock(SerialConfigDTO.class, RETURNS_DEEP_STUBS));
    }
    EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
    when(config.getEndPointConfig()).thenReturn(canbus);
    when(config.getProtocols()).thenReturn("n2k");
    return new Fixture(new TestServer(config), daemon);
  }

  private record Fixture(TestServer server, MockedStatic<MessageDaemon> daemon) implements AutoCloseable {
    CanbusEndPoint endpoint(long id) {
      CanbusEndPoint endpoint = mock(CanbusEndPoint.class);
      when(endpoint.getId()).thenReturn(id);
      when(endpoint.getBoundProtocol()).thenReturn(mock(Protocol.class));
      return endpoint;
    }

    @Override
    public void close() throws IOException {
      try {
        server.close();
      } finally {
        daemon.close();
      }
    }
  }

  private static class TestServer extends CanbusEndPointServer {
    private final Deque<Object> outcomes = new ArrayDeque<>();
    private final List<Runnable> retries = new ArrayList<>();
    private final List<ScheduledFuture<?>> futures = new ArrayList<>();
    private int attempts;
    private IOException bindingFailure;

    TestServer(EndPointServerConfigDTO config) {
      super(mock(AcceptHandler.class), new EndPointURL("canbus://vcan0"), config, mock(EndPointManagerJMX.class));
    }

    @Override
    protected CanbusEndPoint newEndPoint() throws IOException {
      attempts++;
      Object next = outcomes.removeFirst();
      if (next instanceof IOException failure) {
        throw failure;
      }
      return (CanbusEndPoint) next;
    }

    @Override
    protected ScheduledFuture<?> scheduleRetry(Runnable task) {
      retries.add(task);
      ScheduledFuture<?> future = mock(ScheduledFuture.class);
      futures.add(future);
      return future;
    }

    @Override
    protected void bindProtocol(CanbusEndPoint endpoint) throws IOException {
      if (bindingFailure != null) {
        throw bindingFailure;
      }
    }
  }
}
