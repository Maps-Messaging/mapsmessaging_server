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

package io.mapsmessaging.apps;

import io.mapsmessaging.dto.rest.config.BaseManagerConfigDTO;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapsApplicationHostTest {

  @Test
  void emptyDiscoveryDoesNotRequireAnExtension() {
    MapsApplicationHost host = new MapsApplicationHost(List.of());
    host.load(new MapsApplicationContext(null, null, null));
    assertTrue(host.getApplications().isEmpty());
    assertTrue(host.getRestApiPackages().isEmpty());
    assertTrue(host.getConfigProviders().isEmpty());
  }

  @Test
  void readsMetadataBeforeInitializeAndPreservesPriorities() {
    List<String> calls = new ArrayList<>();
    TestApplication application = new TestApplication("twin", calls);
    MapsApplicationHost host = new MapsApplicationHost(List.of(application));
    host.load(new MapsApplicationContext(null, null, null));

    assertEquals(List.of("packages", "configs", "initialize"), calls);
    assertEquals(List.of("io.mapsmessaging.twins.rest"), host.getRestApiPackages());
    assertEquals(1, host.getConfigProviders().size());
    assertSame(application, host.getApplications().get(0));
    assertEquals(550, host.getApplications().get(0).getStartPriority());
    assertEquals(960, host.getApplications().get(0).getStopPriority());
    assertThrows(IllegalStateException.class, () -> host.load(new MapsApplicationContext(null, null, null)));
  }

  @Test
  void rejectsDuplicateAgentNamesWithoutInitializing() {
    List<String> calls = new ArrayList<>();
    MapsApplicationHost host = new MapsApplicationHost(List.of(
        new TestApplication("same", calls), new TestApplication("same", calls)));
    assertThrows(IllegalStateException.class, () -> host.load(new MapsApplicationContext(null, null, null)));
    assertFalse(calls.contains("initialize"));
  }

  @Test
  void startsAndStopsApplicationsByPriority() {
    List<String> calls = new ArrayList<>();
    TestApplication late = new TestApplication("late", calls, 700, 200);
    TestApplication early = new TestApplication("early", calls, 500, 400);
    MapsApplicationHost host = new MapsApplicationHost(List.of(late, early));
    host.load(new MapsApplicationContext(null, null, null));
    calls.clear();

    assertEquals("Application Host", host.getName());
    assertEquals(Status.STOPPED, host.getStatus().getStatus());
    host.start();
    assertEquals(List.of("start:early", "start:late"), calls);
    assertEquals(Status.OK, host.getStatus().getStatus());
    calls.clear();
    host.stop();
    assertEquals(List.of("stop:late", "stop:early"), calls);
    assertEquals(Status.STOPPED, host.getStatus().getStatus());
  }

  @Test
  void refusesToStartBeforeDiscovery() {
    MapsApplicationHost host = new MapsApplicationHost(List.of());
    assertThrows(IllegalStateException.class, host::start);
  }

  private static final class TestApplication implements MapsApplication {
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
    public void initialize(MapsApplicationContext context) {
      calls.add("initialize");
    }
  }
}
