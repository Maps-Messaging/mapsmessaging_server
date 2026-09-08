/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.state.drone.model.GeoPosition;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CanonicalTaskRegistryTest {

  @TempDir
  Path temporaryDirectory;

  @Test
  void duplicateProtocolTaskId_isIdempotent() throws Exception {
    CanonicalTaskRegistry registry = new CanonicalTaskRegistry();
    AtomicInteger notifications = new AtomicInteger();
    registry.addObserver((current, previous) -> notifications.incrementAndGet());

    CanonicalTask first = registry.submit(task("cot-task-1"));
    CanonicalTask duplicate = registry.submit(task("cot-task-1"));

    assertEquals(first.getCanonicalTaskId(), duplicate.getCanonicalTaskId());
    assertEquals(1, registry.list().size());
    assertEquals(1, notifications.get());
  }

  @Test
  void taskIdentityAndLifecycle_surviveRegistryReload() throws Exception {
    CanonicalTaskRegistry registry = new CanonicalTaskRegistry(temporaryDirectory);
    CanonicalTask stored = registry.submit(task("cot-task-1"));
    registry.updateState(stored.getCanonicalTaskId(), CanonicalTaskState.ACTIVE, null);

    CanonicalTaskRegistry reloaded = new CanonicalTaskRegistry(temporaryDirectory);
    CanonicalTask recovered = reloaded.find("cot", "cot-task-1").orElseThrow();

    assertEquals(stored.getCanonicalTaskId(), recovered.getCanonicalTaskId());
    assertEquals(CanonicalTaskState.ACTIVE, recovered.getState());
    assertEquals(47.1, recovered.getTargetPosition().getLatitude());
  }

  @Test
  void terminalState_cannotBeRegressedByReplay() throws Exception {
    CanonicalTaskRegistry registry = new CanonicalTaskRegistry();
    CanonicalTask stored = registry.submit(task("cot-task-1"));
    registry.updateState(stored.getCanonicalTaskId(), CanonicalTaskState.COMPLETED, "arrived");
    registry.updateState(stored.getCanonicalTaskId(), CanonicalTaskState.ACTIVE, null);

    CanonicalTask current = registry.get(stored.getCanonicalTaskId()).orElseThrow();
    assertEquals(CanonicalTaskState.COMPLETED, current.getState());
    assertEquals("arrived", current.getResultReason());
  }

  @Test
  void activeState_cannotBeRegressedByReplay() throws Exception {
    CanonicalTaskRegistry registry = new CanonicalTaskRegistry();
    CanonicalTask stored = registry.submit(task("cot-task-1"));
    registry.updateState(stored.getCanonicalTaskId(), CanonicalTaskState.ACTIVE, null);
    registry.updateState(stored.getCanonicalTaskId(), CanonicalTaskState.PENDING, null);

    CanonicalTask current = registry.get(stored.getCanonicalTaskId()).orElseThrow();
    assertEquals(CanonicalTaskState.ACTIVE, current.getState());
  }

  private CanonicalTask task(String externalId) {
    CanonicalTask task = new CanonicalTask();
    task.setCanonicalTaskId(UUID.randomUUID());
    task.setOriginProtocol("cot");
    task.getProtocolTaskIds().put("cot", externalId);
    task.setTwinId("alpha");
    task.setTaskType(CanonicalTaskType.REPOSITION);
    task.setAction(CanonicalTaskAction.PUSH);
    task.setTargetPosition(new GeoPosition(47.1, 8.2, 20.0, null));
    return task;
  }
}
