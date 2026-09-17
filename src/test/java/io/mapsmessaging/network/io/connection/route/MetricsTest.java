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

package io.mapsmessaging.network.io.connection.route;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.protocol.Protocol;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricsTest {

  @Test
  void firstThroughputSample_hasNoRateYet() {
    EndPointStatus status = mock(EndPointStatus.class);
    when(status.getReadBytesTotal()).thenReturn(1_024L);
    when(status.getWriteBytesTotal()).thenReturn(2_048L);

    Metrics metrics = createMetrics(status, 0L, 0L);

    OptionalDouble throughput = metrics.getThroughputMibPerSecond();

    assertTrue(throughput.isEmpty());
  }

  @Test
  void lastUpdated_usesEndpointMillisecondTimestamp() {
    long lastRead = 1_789_600_000_123L;
    long lastWrite = 1_789_600_001_456L;
    Metrics metrics = createMetrics(mock(EndPointStatus.class), lastRead, lastWrite);

    assertEquals(Instant.ofEpochMilli(lastWrite), metrics.getLastUpdated());
  }

  @Test
  void noProtocol_hasNoThroughputSample() {
    EndPointConnection connection = mock(EndPointConnection.class);
    when(connection.getProtocol()).thenReturn(null);

    Metrics metrics = new Metrics(connection);

    assertTrue(metrics.getThroughputMibPerSecond().isEmpty());
  }

  private Metrics createMetrics(EndPointStatus status, long lastRead, long lastWrite) {
    EndPointConnection connection = mock(EndPointConnection.class);
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);

    when(connection.getProtocol()).thenReturn(protocol);
    when(protocol.getEndPoint()).thenReturn(endPoint);
    when(endPoint.getEndPointStatus()).thenReturn(status);
    when(endPoint.getLastRead()).thenReturn(lastRead);
    when(endPoint.getLastWrite()).thenReturn(lastWrite);

    return new Metrics(connection);
  }
}
