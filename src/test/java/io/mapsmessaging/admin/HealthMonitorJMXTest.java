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

package io.mapsmessaging.admin;

import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.management.openmbean.TabularData;
import java.util.List;

class HealthMonitorJMXTest {

  @Test
  void healthList_collectsStatuses_andCurrentStatusContainsOnlyNonInfoMessages() {
    JMXManager manager = Mockito.mock(JMXManager.class);
    HealthStatus infoStatus = new HealthStatus("broker", HealthStatus.LEVEL.INFO, "healthy", "broker:name=test");
    HealthStatus warningStatus = new HealthStatus("disk", HealthStatus.LEVEL.WARN, "disk nearly full", "disk:name=data");
    HealthStatus errorStatus = new HealthStatus("network", HealthStatus.LEVEL.ERROR, "network unavailable", "network:name=primary");
    HealthMonitor infoMonitor = () -> infoStatus;
    HealthMonitor warningMonitor = () -> warningStatus;
    HealthMonitor errorMonitor = () -> errorStatus;
    Mockito.when(manager.getHealthList()).thenReturn(List.of(infoMonitor, warningMonitor, errorMonitor));

    try (MockedStatic<JMXManager> jmxManager = Mockito.mockStatic(JMXManager.class)) {
      jmxManager.when(JMXManager::getInstance).thenReturn(manager);
      HealthMonitorJMX healthMonitorJMX = new HealthMonitorJMX(List.of("type=Broker"));

      List<HealthStatus> statuses = healthMonitorJMX.healthList();

      Assertions.assertEquals(3, statuses.size());
      Assertions.assertEquals(List.of(infoStatus, warningStatus, errorStatus), statuses);
      Assertions.assertEquals("disk nearly full,network unavailable,", healthMonitorJMX.getCurrentStatus());
    }
  }

  @Test
  void health_withNoMonitors_returnsEmptyTabularDataWithExpectedColumns() throws Exception {
    JMXManager manager = Mockito.mock(JMXManager.class);
    Mockito.when(manager.getHealthList()).thenReturn(List.of());

    try (MockedStatic<JMXManager> jmxManager = Mockito.mockStatic(JMXManager.class)) {
      jmxManager.when(JMXManager::getInstance).thenReturn(manager);
      HealthMonitorJMX healthMonitorJMX = new HealthMonitorJMX(List.of("type=Broker"));

      TabularData health = healthMonitorJMX.health();

      Assertions.assertTrue(health.isEmpty());
      Assertions.assertEquals(
          List.of("healthId", "level", "message", "resource"),
          health.getTabularType().getIndexNames());
      Assertions.assertEquals("", healthMonitorJMX.getCurrentStatus());
    }
  }
}
