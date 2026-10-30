/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.protocol.impl.canbus;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.canbus.CanbusEndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.n2k.N2kProtocol;
import io.mapsmessaging.network.protocol.impl.canaerospace.CanaerospaceProtocol;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CanbusProtocolLifecycleTest {

  @Test
  void n2k_close_removes_session_and_cancels_periodic_work_once() throws Exception {
    try (Fixture fixture = fixture(true)) {
      fixture.protocol.close();
      fixture.protocol.close();
      verify(fixture.sessions).close(fixture.session, false);
      verify(fixture.endpoint).close();
      verify(fixture.periodic).cancel(false);
    }
  }

  @Test
  void canaerospace_close_removes_session_once() throws Exception {
    try (Fixture fixture = fixture(false)) {
      fixture.protocol.close();
      fixture.protocol.close();
      verify(fixture.sessions).close(fixture.session, false);
      verify(fixture.endpoint).close();
    }
  }

  @Test
  void session_close_failure_still_releases_endpoint_for_both_protocols() throws Exception {
    for (boolean n2k : new boolean[]{true, false}) {
      try (Fixture fixture = fixture(n2k)) {
        IOException failure = new IOException("session close failed");
        doThrow(failure).when(fixture.sessions).close(fixture.session, false);
        assertSame(failure, assertThrows(IOException.class, fixture.protocol::close));
        verify(fixture.endpoint).close();
      }
    }
  }

  @Test
  void close_stops_and_joins_each_inbound_worker() throws Exception {
    for (boolean n2k : new boolean[]{true, false}) {
      try (Fixture fixture = fixture(n2k)) {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Runnable worker;
        if (n2k) {
          N2kProtocol protocol = (N2kProtocol) fixture.protocol;
          doAnswer(call -> waitForRead(entered, release)).when(protocol).processPacket(any(Packet.class));
          io.mapsmessaging.network.protocol.impl.n2k.InboundProcessor processor =
              new io.mapsmessaging.network.protocol.impl.n2k.InboundProcessor(protocol);
          set(protocol, N2kProtocol.class, "inboundProcessor", processor);
          worker = processor;
        } else {
          CanaerospaceProtocol protocol = (CanaerospaceProtocol) fixture.protocol;
          doAnswer(call -> waitForRead(entered, release)).when(protocol).processPacket(any(Packet.class));
          io.mapsmessaging.network.protocol.impl.canaerospace.InboundProcessor processor =
              new io.mapsmessaging.network.protocol.impl.canaerospace.InboundProcessor(protocol);
          set(protocol, CanaerospaceProtocol.class, "inboundProcessor", processor);
          worker = processor;
        }
        Thread thread = new Thread(worker, "canbus-lifecycle-test");
        Class<?> owner = n2k ? N2kProtocol.class : CanaerospaceProtocol.class;
        set(fixture.protocol, owner, "inboundThread", thread);
        thread.start();
        try {
          assertTrue(entered.await(5, TimeUnit.SECONDS));
          fixture.protocol.close();
          assertFalse(thread.isAlive());
        } finally {
          release.countDown();
          thread.interrupt();
          thread.join(1000);
        }
      }
    }
  }

  @Test
  void reader_failure_cleanup_does_not_interrupt_its_own_session_close() throws Exception {
    for (boolean n2k : new boolean[]{true, false}) {
      try (Fixture fixture = fixture(n2k)) {
        set(fixture.protocol, n2k ? N2kProtocol.class : CanaerospaceProtocol.class,
            "inboundThread", Thread.currentThread());
        doAnswer(call -> {
          assertFalse(Thread.currentThread().isInterrupted());
          return null;
        }).when(fixture.sessions).close(fixture.session, false);
        try {
          fixture.protocol.close();
          assertFalse(Thread.currentThread().isInterrupted());
        } finally {
          Thread.interrupted();
        }
      }
    }
  }

  private static boolean waitForRead(CountDownLatch entered, CountDownLatch release) throws IOException {
    entered.countDown();
    try {
      release.await();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException(exception);
    }
    return true;
  }

  private static Fixture fixture(boolean n2k) throws Exception {
    MockedStatic<MessageDaemon> daemon = mockStatic(MessageDaemon.class);
    daemon.when(MessageDaemon::getInstance).thenReturn(mock(MessageDaemon.class, RETURNS_DEEP_STUBS));
    MockedStatic<SessionManager> sessionApi = mockStatic(SessionManager.class);
    SessionManager sessions = mock(SessionManager.class);
    sessionApi.when(SessionManager::getInstance).thenReturn(sessions);
    Protocol protocol = n2k ? mock(N2kProtocol.class, CALLS_REAL_METHODS)
        : mock(CanaerospaceProtocol.class, CALLS_REAL_METHODS);
    Class<?> owner = n2k ? N2kProtocol.class : CanaerospaceProtocol.class;
    Session session = mock(Session.class);
    CanbusEndPoint endpoint = mock(CanbusEndPoint.class);
    set(protocol, owner, "session", session);
    set(protocol, owner, "logger", mock(Logger.class));
    set(protocol, Protocol.class, "endPoint", endpoint);
    ScheduledFuture<?> periodic = mock(ScheduledFuture.class);
    if (n2k) {
      set(protocol, owner, "inboundProcessor", mock(io.mapsmessaging.network.protocol.impl.n2k.InboundProcessor.class));
      set(protocol, owner, "scheduledFuture", periodic);
    } else {
      set(protocol, owner, "inboundProcessor", mock(io.mapsmessaging.network.protocol.impl.canaerospace.InboundProcessor.class));
    }
    return new Fixture(protocol, session, sessions, endpoint, periodic, sessionApi, daemon);
  }

  private static void set(Object target, Class<?> owner, String name, Object value) throws Exception {
    // Mockito's inline mock preserves the production class; accept explicit owners only.
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private record Fixture(Protocol protocol, Session session, SessionManager sessions,
      CanbusEndPoint endpoint, ScheduledFuture<?> periodic, MockedStatic<SessionManager> sessionApi,
      MockedStatic<MessageDaemon> daemon) implements AutoCloseable {
    @Override
    public void close() {
      sessionApi.close();
      daemon.close();
    }
  }
}
