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

package io.mapsmessaging.state;

import io.mapsmessaging.utilities.Lifecycle;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StateManagerAgentShutdownTest {

  @Test
  void shutdown_stops_components_in_reverse_startup_order_without_restarting() throws Exception {
    List<String> calls = new ArrayList<>();
    StateManagerAgent agent = agentWith(component("first", calls, false), component("second", calls, false));
    agent.start();
    agent.stop();
    assertEquals(List.of("start:first", "start:second", "stop:second", "stop:first"), calls);
  }

  @Test
  void shutdown_continues_after_a_component_fails() throws Exception {
    List<String> calls = new ArrayList<>();
    StateManagerAgent agent = agentWith(component("first", calls, false), component("failing", calls, true),
        component("last", calls, false));
    assertDoesNotThrow(agent::stop);
    assertEquals(List.of("stop:last", "stop:failing", "stop:first"), calls);
  }

  @Test
  void shutdown_without_configured_components_is_safe() throws Exception {
    assertDoesNotThrow(agentWith()::stop);
  }

  @SuppressWarnings("unchecked")
  private StateManagerAgent agentWith(Lifecycle... components) throws Exception {
    ConfigurationManager config = mock(ConfigurationManager.class);
    StateManagerAgent agent;
    try (MockedStatic<ConfigurationManager> configuration = mockStatic(ConfigurationManager.class)) {
      configuration.when(ConfigurationManager::getInstance).thenReturn(config);
      agent = new StateManagerAgent();
    }
    Field field = StateManagerAgent.class.getDeclaredField("lifecycleList");
    field.setAccessible(true);
    ((List<Lifecycle>) field.get(agent)).addAll(List.of(components));
    return agent;
  }

  private Lifecycle component(String name, List<String> calls, boolean failOnStop) {
    return new Lifecycle() {
      @Override
      public void start() {
        calls.add("start:" + name);
      }

      @Override
      public void stop() {
        calls.add("stop:" + name);
        if (failOnStop) {
          throw new IllegalStateException("Cannot stop " + name);
        }
      }
    };
  }
}
