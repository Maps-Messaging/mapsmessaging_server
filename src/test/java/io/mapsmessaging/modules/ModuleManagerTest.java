/*
 *
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

package io.mapsmessaging.modules;

import io.mapsmessaging.dto.rest.config.BaseManagerConfigDTO;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModuleManagerTest {

  @Test
  void emptyDiscoveryDoesNotRequireAnExtension() {
    ModuleManager host = new ModuleManager(List.of());
    host.load(new ModuleContext(null, null, null));
    assertTrue(host.getApplications().isEmpty());
    assertTrue(host.getRestApiPackages().isEmpty());
    assertTrue(host.getConfigProviders().isEmpty());
  }

  @Test
  void readsMetadataBeforeInitializeAndPreservesPriorities() {
    List<String> calls = new ArrayList<>();
    TestApplication application = new TestApplication("twin", calls);
    ModuleManager host = new ModuleManager(List.of(application));
    host.load(new ModuleContext(null, null, null));

    assertEquals(List.of("packages", "configs", "initialize"), calls);
    assertEquals(List.of("io.mapsmessaging.twins.rest"), host.getRestApiPackages());
    assertEquals(1, host.getConfigProviders().size());
    assertSame(application, host.getApplications().get(0));
    assertEquals(550, host.getApplications().get(0).getStartPriority());
    assertEquals(960, host.getApplications().get(0).getStopPriority());
    assertThrows(IllegalStateException.class, () -> host.load(new ModuleContext(null, null, null)));
  }

  @Test
  void metadataIsAvailableBeforeInitializationAndCanBeReadRepeatedly() {
    List<String> calls = new ArrayList<>();
    TestApplication application = new TestApplication("twin", calls);
    ModuleManager manager = new ModuleManager(List.of(application));

    manager.discover();
    assertEquals(List.of("packages", "configs"), calls);
    assertEquals(List.of("io.mapsmessaging.twins.rest"), manager.getRestApiPackages());
    assertEquals(List.of("io.mapsmessaging.twins.rest"), manager.getRestApiPackages());
    assertEquals(1, manager.getConfigProviders().size());
    assertEquals(1, manager.getConfigProviders().size());
    assertThrows(IllegalStateException.class, manager::discover);

    manager.initialize(new ModuleContext(null, null, null));
    assertEquals(List.of("packages", "configs", "initialize"), calls);
    assertThrows(IllegalStateException.class, () -> manager.initialize(new ModuleContext(null, null, null)));
  }

  @Test
  void initializationRequiresDiscovery() {
    ModuleManager manager = new ModuleManager(List.of());
    assertThrows(IllegalStateException.class, () -> manager.initialize(new ModuleContext(null, null, null)));
  }

  @Test
  void rejectsDuplicateAgentNamesWithoutInitializing() {
    List<String> calls = new ArrayList<>();
    ModuleManager host = new ModuleManager(List.of(
        new TestApplication("same", calls), new TestApplication("same", calls)));
    assertThrows(IllegalStateException.class, () -> host.load(new ModuleContext(null, null, null)));
    assertFalse(calls.contains("initialize"));
  }

  @Test
  void moduleManagerDoesNotOwnApplicationLifecycle() {
    List<String> calls = new ArrayList<>();
    TestApplication first = new TestApplication("first", calls, 700, 200);
    TestApplication second = new TestApplication("second", calls, 500, 400);
    ModuleManager manager = new ModuleManager(List.of(first, second));
    manager.load(new ModuleContext(null, null, null));
    calls.clear();

    assertEquals(List.of(first, second), manager.getApplications());
    assertEquals(700, first.getStartPriority());
    assertEquals(500, second.getStartPriority());
    assertEquals(200, first.getStopPriority());
    assertEquals(400, second.getStopPriority());
    assertTrue(calls.isEmpty());
  }

  private static final class TestApplication implements MapsModule {
    private final String name;
    private final List<String> calls;
    private final int startPriority;
    private final int stopPriority;

    private TestApplication(String name, List<String> calls) {
      this(name, calls, 550, 960);
    }

    private TestApplication(String name, List<String> calls, int startPriority, int stopPriority) {
      this.name = name;
      this.calls = calls;
      this.startPriority = startPriority;
      this.stopPriority = stopPriority;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getDescription() {
      return name;
    }

    @Override
    public SubSystemStatusDTO getStatus() {
      SubSystemStatusDTO status = new SubSystemStatusDTO();
      status.setStatus(Status.OK);
      return status;
    }

    @Override
    public void start() {
      calls.add("start:" + name);
    }

    @Override
    public void stop() {
      calls.add("stop:" + name);
    }

    @Override
    public int getStartPriority() {
      return startPriority;
    }

    @Override
    public int getStopPriority() {
      return stopPriority;
    }

    @Override
    public List<String> getRestApiPackages() {
      calls.add("packages");
      return List.of("io.mapsmessaging.twins.rest");
    }

    @Override
    public List<ManagerConfigProvider> getConfigProviders() {
      calls.add("configs");
      return List.of(new ManagerConfigProvider() {
        @Override
        public String getType() {
          return "test";
        }

        @Override
        public Class<? extends BaseManagerConfigDTO> getConfigClass() {
          return BaseManagerConfigDTO.class;
        }
      });
    }

    @Override
    public void initialize(ModuleContext context) {
      calls.add("initialize");
    }
  }
}
