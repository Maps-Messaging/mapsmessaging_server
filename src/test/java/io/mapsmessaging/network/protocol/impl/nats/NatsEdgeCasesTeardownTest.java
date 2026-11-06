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

package io.mapsmessaging.network.protocol.impl.nats;

import io.nats.client.Connection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NatsEdgeCasesTeardownTest {

  @Test
  void teardown_closes_connection_when_live_drain_times_out() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.getStatus()).thenReturn(Connection.Status.CONNECTED);
    TimeoutException failure = new TimeoutException("Drain timed out");
    doThrow(failure).when(connection).drain(Duration.ofSeconds(2));

    NatsEdgeCasesTest test = testWithConnection(connection);

    assertSame(failure, assertThrows(TimeoutException.class, test::teardown));
    verify(connection).close();
  }

  @ParameterizedTest
  @EnumSource(value = Connection.Status.class, names = {"CLOSED", "DISCONNECTED", "RECONNECTING"})
  void teardown_closes_without_draining_unavailable_connection(Connection.Status status) throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.getStatus()).thenReturn(status);

    assertDoesNotThrow(testWithConnection(connection)::teardown);

    verify(connection, never()).drain(any(Duration.class));
    verify(connection).close();
  }

  @Test
  void teardown_closes_after_connection_is_closed_during_drain() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.getStatus()).thenReturn(Connection.Status.CONNECTED, Connection.Status.CLOSED);
    TimeoutException failure = new TimeoutException("Connection closed while draining");
    failure.initCause(new CancellationException());
    doThrow(failure).when(connection).drain(Duration.ofSeconds(2));

    assertDoesNotThrow(testWithConnection(connection)::teardown);

    verify(connection).close();
  }

  @Test
  void teardown_closes_after_disconnection_makes_drain_illegal() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.getStatus()).thenReturn(Connection.Status.CONNECTED, Connection.Status.DISCONNECTED);
    doThrow(new IllegalStateException("Connection disconnected")).when(connection).drain(Duration.ofSeconds(2));

    assertDoesNotThrow(testWithConnection(connection)::teardown);

    verify(connection).close();
  }

  @Test
  void teardown_preserves_illegal_state_failure_for_live_connection() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.getStatus()).thenReturn(Connection.Status.CONNECTED);
    IllegalStateException failure = new IllegalStateException("Unexpected drain failure");
    doThrow(failure).when(connection).drain(Duration.ofSeconds(2));

    assertSame(failure, assertThrows(IllegalStateException.class, testWithConnection(connection)::teardown));

    verify(connection).close();
  }

  @Test
  void teardown_closes_after_successful_drain() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.getStatus()).thenReturn(Connection.Status.CONNECTED);

    assertDoesNotThrow(testWithConnection(connection)::teardown);

    var cleanupOrder = inOrder(connection);
    cleanupOrder.verify(connection).drain(Duration.ofSeconds(2));
    cleanupOrder.verify(connection).close();
  }

  @Test
  void teardown_accepts_absent_connection() {
    assertDoesNotThrow(new NatsEdgeCasesTest()::teardown);
  }

  private NatsEdgeCasesTest testWithConnection(Connection connection) throws Exception {
    NatsEdgeCasesTest test = new NatsEdgeCasesTest();
    Field field = NatsEdgeCasesTest.class.getDeclaredField("connection");
    field.setAccessible(true);
    field.set(test, connection);
    return test;
  }
}
