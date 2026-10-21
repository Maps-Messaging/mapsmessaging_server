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

package io.mapsmessaging.state.metrics;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Test;
import javax.management.ObjectName;
import static org.junit.jupiter.api.Assertions.*;

class TwinMetricsManagerTest {
  @Test
  void start_registers_all_type_lifecycle_pairs_and_stop_removes_them() throws Exception {
    boolean enabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(true);
    var constructor = JMXManager.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    JMXManager isolatedManager = constructor.newInstance();
    var server = javax.management.MBeanServerFactory.newMBeanServer();
    var serverField = JMXManager.class.getDeclaredField("mbs");
    serverField.setAccessible(true);
    serverField.set(isolatedManager, server);
    TwinMetricsManager manager = new TwinMetricsManager(new TwinManager());
    var pattern = new ObjectName("io.mapsmessaging:type=Integration, name=Twins,*");
    try (var mocked = org.mockito.Mockito.mockStatic(JMXManager.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
      mocked.when(JMXManager::getInstance).thenReturn(isolatedManager);
      try {
        assertTrue(server.queryNames(pattern, null).isEmpty());
        for (int cycle = 0; cycle < 2; cycle++) {
          manager.start();
          assertEquals(TwinType.values().length * TwinLifecycleStatus.values().length,
              server.queryNames(pattern, null).size());
          for (var name : server.queryNames(pattern, null)) {
            assertEquals(0L, server.getAttribute(name, "Twin Count"));
          }
          manager.stop();
          assertTrue(server.queryNames(pattern, null).isEmpty());
        }
        manager.stop();
      } finally {
        manager.stop();
      }
    } finally {
      JMXManager.setEnableJMX(enabled);
    }
  }

  @Test
  void disabled_jmx_allows_start_and_repeated_stop() {
    boolean enabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    TwinMetricsManager manager = new TwinMetricsManager(new TwinManager());
    try {
      manager.start();
      manager.stop();
      manager.stop();
    } finally {
      manager.stop();
      JMXManager.setEnableJMX(enabled);
    }
  }
}
