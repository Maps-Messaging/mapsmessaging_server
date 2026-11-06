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

package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisconnectedCancellationRaceTest {

  @Test
  void cancellation_between_eligibility_check_and_endpoint_publication_closes_pending_transport() throws Exception {
    EndPointConnection connection = mock(EndPointConnection.class);
    EndPointConnectionFactory factory = mock(EndPointConnectionFactory.class);
    EndPoint endpoint = mock(EndPoint.class);
    Disconnected state = new Disconnected(connection);
    when(connection.getUrl()).thenReturn(new EndPointURL("tcp://127.0.0.1:1883"));
    when(connection.getEndPointConnectionFactory()).thenReturn(factory);
    when(connection.getJMXPath()).thenReturn(List.of());
    when(connection.getState()).thenReturn(state);
    when(connection.isRunning()).thenReturn(true);
    when(factory.connect(any(), any(), any(), any(), any())).thenReturn(endpoint);
    CountDownLatch checkReached = new CountDownLatch(1);
    CountDownLatch cancellationFinished = new CountDownLatch(1);
    when(connection.isPaused()).thenAnswer(invocation -> {
      checkReached.countDown();
      assertTrue(cancellationFinished.await(5, TimeUnit.SECONDS));
      return false;
    });

    try (var executor = Executors.newSingleThreadExecutor()) {
      var execution = executor.submit(state::execute);
      try {
        assertTrue(checkReached.await(5, TimeUnit.SECONDS));
        state.cancel();
      } finally {
        cancellationFinished.countDown();
      }
      execution.get(5, TimeUnit.SECONDS);
    }

    assertNull(state.getActiveEndPoint());
    verify(endpoint).close();
  }
}
