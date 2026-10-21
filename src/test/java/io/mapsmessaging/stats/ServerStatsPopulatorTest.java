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

package io.mapsmessaging.stats;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.SubSystemManager;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.NetworkManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.stats.data.ConnectionStats;
import io.mapsmessaging.stats.data.ServerStats;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.LongAdder;

class ServerStatsPopulatorTest {

  @Test
  void collect_populatesIdentityRuntimeMetricsAndAggregatedConnections() {
    long originalPacketsSent = EndPointServerStatus.SystemTotalPacketsSent.sum();
    long originalPacketsReceived = EndPointServerStatus.SystemTotalPacketsReceived.sum();
    long originalBytesReceived = EndPointServerStatus.SystemTotalBytesReceived.sum();
    long originalBytesSent = EndPointServerStatus.SystemTotalBytesSent.sum();
    long originalFailedConnections = EndPointServerStatus.SystemTotalFailedConnections.sum();

    try {
      setCounter(EndPointServerStatus.SystemTotalPacketsSent, 17);
      setCounter(EndPointServerStatus.SystemTotalPacketsReceived, 19);
      setCounter(EndPointServerStatus.SystemTotalBytesReceived, 23);
      setCounter(EndPointServerStatus.SystemTotalBytesSent, 29);
      setCounter(EndPointServerStatus.SystemTotalFailedConnections, 31);

      EndPointServer firstServer = mockEndPointServer(2, 37);
      EndPointServer secondServer = mockEndPointServer(3, 41);
      EndPointManager firstManager = Mockito.mock(EndPointManager.class);
      EndPointManager secondManager = Mockito.mock(EndPointManager.class);
      Mockito.when(firstManager.getEndPointServer()).thenReturn(firstServer);
      Mockito.when(secondManager.getEndPointServer()).thenReturn(secondServer);

      NetworkManager networkManager = Mockito.mock(NetworkManager.class);
      Mockito.when(networkManager.getAll()).thenReturn(List.of(firstManager, secondManager));
      SubSystemManager subSystemManager = Mockito.mock(SubSystemManager.class);
      Mockito.when(subSystemManager.getNetworkManager()).thenReturn(networkManager);
      MessageDaemon daemon = Mockito.mock(MessageDaemon.class);
      Mockito.when(daemon.getSubSystemManager()).thenReturn(subSystemManager);

      long beforeCollection = System.currentTimeMillis();
      try (MockedStatic<MessageDaemon> messageDaemon = Mockito.mockStatic(MessageDaemon.class)) {
        messageDaemon.when(MessageDaemon::getInstance).thenReturn(daemon);

        ServerStats stats = ServerStatsPopulator.collect(
            "server-id", "server-name", "license-id", "1.2.3", 12_345);

        long afterCollection = System.currentTimeMillis();
        Assertions.assertAll(
            () -> Assertions.assertEquals("server-id", stats.getServerId()),
            () -> Assertions.assertEquals("server-name", stats.getServerName()),
            () -> Assertions.assertEquals("license-id", stats.getLicenseId()),
            () -> Assertions.assertTrue(stats.getTimestamp() >= beforeCollection),
            () -> Assertions.assertTrue(stats.getTimestamp() <= afterCollection),
            () -> Assertions.assertEquals("1.2.3", stats.getVersion().getServerVersion()),
            () -> Assertions.assertEquals(System.getProperty("os.name"), stats.getVersion().getOsName()),
            () -> Assertions.assertEquals(System.getProperty("java.version"), stats.getVersion().getJvmVersion()),
            () -> Assertions.assertNotNull(stats.getVersion().getHostname()),
            () -> Assertions.assertTrue(stats.getMemory().getTotalJvmMemoryMb() > 0),
            () -> Assertions.assertTrue(stats.getMemory().getHeapUsedMb() >= 0),
            () -> Assertions.assertTrue(stats.getDisk().getTotalDiskMb() >= stats.getDisk().getFreeDiskMb()),
            () -> Assertions.assertEquals(12_345, stats.getUptime().getServerUptimeSecs()),
            () -> Assertions.assertTrue(stats.getUptime().getSystemUptimeSecs() >= 0),
            () -> Assertions.assertTrue(stats.getNetwork().getInterfaceCount() >= 0),
            () -> Assertions.assertTrue(
                stats.getNetwork().getActiveInterfaceCount() <= stats.getNetwork().getInterfaceCount()),
            () -> assertConnectionStats(stats.getConnections())
        );
      }
    } finally {
      setCounter(EndPointServerStatus.SystemTotalPacketsSent, originalPacketsSent);
      setCounter(EndPointServerStatus.SystemTotalPacketsReceived, originalPacketsReceived);
      setCounter(EndPointServerStatus.SystemTotalBytesReceived, originalBytesReceived);
      setCounter(EndPointServerStatus.SystemTotalBytesSent, originalBytesSent);
      setCounter(EndPointServerStatus.SystemTotalFailedConnections, originalFailedConnections);
    }
  }

  private EndPointServer mockEndPointServer(int activeConnections, long errors) {
    EndPointServer server = Mockito.mock(EndPointServer.class);
    List<EndPoint> endPoints = java.util.Collections.nCopies(activeConnections, Mockito.mock(EndPoint.class));
    Mockito.when(server.getActiveEndPoints()).thenReturn(endPoints);
    Mockito.when(server.getTotalErrors()).thenReturn(errors);
    return server;
  }

  private void assertConnectionStats(ConnectionStats connections) {
    Assertions.assertAll(
        () -> Assertions.assertEquals(5, connections.getCurrentConnections()),
        () -> Assertions.assertEquals(109, connections.getErrors()),
        () -> Assertions.assertEquals(19, connections.getPacketsIn()),
        () -> Assertions.assertEquals(17, connections.getPacketsOut()),
        () -> Assertions.assertEquals(23, connections.getBytesIn()),
        () -> Assertions.assertEquals(29, connections.getBytesOut())
    );
  }

  private void setCounter(LongAdder counter, long value) {
    counter.reset();
    counter.add(value);
  }
}
