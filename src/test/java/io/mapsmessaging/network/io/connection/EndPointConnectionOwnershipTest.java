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

package io.mapsmessaging.network.io.connection;

import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.connection.state.*;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EndPointConnectionOwnershipTest {

  @ParameterizedTest
  @ValueSource(strings = {"tcp", "ssl"})
  void late_old_endpoint_close_preserves_replacement(String transport) throws Exception {
    try (Fixture fixture = new Fixture(transport)) {
      EndPoint old = mock(EndPoint.class);
      EndPoint replacement = mock(EndPoint.class);
      fixture.accept(old);
      CountDownLatch callbackReady = new CountDownLatch(1);
      CountDownLatch releaseCallback = new CountDownLatch(1);
      try (var executor = Executors.newSingleThreadExecutor()) {
        var callback = executor.submit(() -> {
          callbackReady.countDown();
          assertTrue(releaseCallback.await(5, TimeUnit.SECONDS));
          fixture.connection.handleCloseEndPoint(old);
          return null;
        });
        try {
          assertTrue(callbackReady.await(5, TimeUnit.SECONDS));
          fixture.accept(replacement);
          State replacementState = fixture.connection.getState();
          releaseCallback.countDown();
          callback.get(5, TimeUnit.SECONDS);
          assertSame(replacement, fixture.connection.getEndPoint());
          assertSame(replacementState, fixture.connection.getState());
          verify(replacement, never()).close();
        } finally {
          releaseCallback.countDown();
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"tcp", "ssl"})
  void old_protocol_completion_during_new_handshake_cannot_replace_endpoint(String transport) throws Exception {
    try (Fixture fixture = new Fixture(transport)) {
      EndPoint old = mock(EndPoint.class);
      EndPoint replacement = mock(EndPoint.class);
      fixture.accept(old);
      fixture.handshake(replacement);
      doAnswer(invocation -> {
        fixture.connection.handleCloseEndPoint(old);
        return null;
      }).when(old).close();
      State handshakeState = fixture.connection.getState();
      fixture.connection.handleNewEndPoint(old);
      assertSame(handshakeState, fixture.connection.getState());
      verify(old).close();
      fixture.connection.handleNewEndPoint(replacement);
      assertSame(replacement, fixture.connection.getEndPoint());
      assertInstanceOf(Connected.class, fixture.connection.getState());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"tcp", "ssl"})
  void current_close_reconnects_and_duplicate_close_is_ignored(String transport) throws Exception {
    try (Fixture fixture = new Fixture(transport)) {
      EndPoint endpoint = mock(EndPoint.class);
      fixture.accept(endpoint);
      fixture.connection.handleCloseEndPoint(endpoint);
      assertNull(fixture.connection.getEndPoint());
      State delayed = fixture.connection.getState();
      assertInstanceOf(Delayed.class, delayed);
      fixture.connection.handleCloseEndPoint(endpoint);
      assertSame(delayed, fixture.connection.getState());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"tcp", "ssl"})
  void current_protocol_handshake_failure_reconnects(String transport) throws Exception {
    try (Fixture fixture = new Fixture(transport)) {
      EndPoint endpoint = mock(EndPoint.class);
      fixture.handshake(endpoint);
      fixture.connection.handleCloseEndPoint(endpoint);
      assertInstanceOf(Delayed.class, fixture.connection.getState());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"tcp", "ssl"})
  void pending_transport_failure_reconnects_before_protocol_is_created(String transport) throws Exception {
    try (Fixture fixture = new Fixture(transport)) {
      EndPoint endpoint = mock(EndPoint.class);
      when(fixture.factory.connect(any(), any(), any(), any(), any())).thenReturn(endpoint);
      Disconnected attempt = new Disconnected(fixture.connection);
      fixture.connection.scheduleState(attempt);
      attempt.execute();
      fixture.connection.handleCloseEndPoint(endpoint);
      assertInstanceOf(Delayed.class, fixture.connection.getState());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"tcp", "ssl"})
  void close_while_paused_does_not_reconnect_and_resume_does(String transport) throws Exception {
    try (Fixture fixture = new Fixture(transport)) {
      EndPoint endpoint = mock(EndPoint.class);
      fixture.accept(endpoint);
      fixture.connection.pause();
      State pausedState = fixture.connection.getState();
      fixture.connection.handleCloseEndPoint(endpoint);
      assertNull(fixture.connection.getEndPoint());
      assertSame(pausedState, fixture.connection.getState());
      fixture.connection.resume();
      assertInstanceOf(Delayed.class, fixture.connection.getState());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"tcp", "ssl"})
  void close_after_stop_preserves_shutdown(String transport) throws Exception {
    try (Fixture fixture = new Fixture(transport)) {
      EndPoint endpoint = mock(EndPoint.class);
      fixture.accept(endpoint);
      fixture.connection.stop();
      State shutdown = fixture.connection.getState();
      fixture.connection.handleCloseEndPoint(endpoint);
      assertNull(fixture.connection.getEndPoint());
      assertSame(shutdown, fixture.connection.getState());
      assertInstanceOf(Shutdown.class, shutdown);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"tcp", "ssl"})
  void protocol_completion_while_paused_cannot_advance_state(String transport) throws Exception {
    try (Fixture fixture = new Fixture(transport)) {
      EndPoint endpoint = mock(EndPoint.class);
      fixture.handshake(endpoint);
      fixture.connection.pause();
      State pausedState = fixture.connection.getState();
      fixture.connection.handleNewEndPoint(endpoint);
      assertSame(pausedState, fixture.connection.getState());
      verify(endpoint).close();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"tcp", "ssl"})
  void stale_close_during_replacement_handshake_is_ignored(String transport) throws Exception {
    try (Fixture fixture = new Fixture(transport)) {
      EndPoint old = mock(EndPoint.class);
      EndPoint replacement = mock(EndPoint.class);
      fixture.handshake(replacement);
      State handshake = fixture.connection.getState();
      fixture.connection.handleCloseEndPoint(old);
      assertSame(handshake, fixture.connection.getState());
      fixture.connection.handleNewEndPoint(replacement);
      assertSame(replacement, fixture.connection.getEndPoint());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"tcp", "ssl"})
  void protocol_completion_after_stop_preserves_shutdown(String transport) throws Exception {
    try (Fixture fixture = new Fixture(transport)) {
      EndPoint endpoint = mock(EndPoint.class);
      fixture.handshake(endpoint);
      fixture.connection.stop();
      State shutdown = fixture.connection.getState();
      fixture.connection.handleNewEndPoint(endpoint);
      assertSame(shutdown, fixture.connection.getState());
      verify(endpoint).close();
    }
  }

  private static class Fixture implements AutoCloseable {
    final MockedStatic<SimpleTaskScheduler> scheduler;
    final EndPointConnectionFactory factory = mock(EndPointConnectionFactory.class);
    final EndPointConnection connection;

    Fixture(String transport) {
      scheduler = mockStatic(SimpleTaskScheduler.class);
      scheduler.when(SimpleTaskScheduler::getInstance).thenReturn(mock(SimpleTaskScheduler.class));
      EndPointConnectionServerConfigDTO config = new EndPointConnectionServerConfigDTO();
      config.setProtocolConfigs(List.of());
      config.setName("ownership");
      config.setLinkConfigs(List.of());
      connection = new EndPointConnection(new EndPointURL(transport + "://127.0.0.1:1883"),
          config, factory, mock(SelectorLoadManager.class), null);
      connection.start();
    }

    void handshake(EndPoint endpoint) {
      connection.setEndPoint(null);
      Protocol protocol = mock(Protocol.class);
      when(protocol.getEndPoint()).thenReturn(endpoint);
      connection.setProtocol(protocol);
      connection.scheduleState(new Connecting(connection));
    }

    void accept(EndPoint endpoint) throws Exception {
      handshake(endpoint);
      connection.handleNewEndPoint(endpoint);
    }

    @Override
    public void close() {
      try {
        connection.close();
      } finally {
        scheduler.close();
      }
    }
  }
}
