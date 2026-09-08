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
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.state.config.cot.CotManagedPlatformConfigDTO;
import io.mapsmessaging.state.config.cot.CotTwinConfigDTO;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakDetail;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.state.drone.tak.model.TakPoint;
import io.mapsmessaging.state.drone.tak.model.TakTask;
import io.mapsmessaging.state.task.CanonicalTask;
import io.mapsmessaging.state.task.CanonicalTaskAction;
import io.mapsmessaging.state.task.CanonicalTaskRegistry;
import io.mapsmessaging.state.task.CanonicalTaskState;
import io.mapsmessaging.state.task.CanonicalTaskType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CotTaskProfileTest {

  @Test
  void reposition_mapsToCanonicalTaskAndConvertsAltitude() throws Exception {
    CanonicalTaskRegistry tasks = new CanonicalTaskRegistry();
    CotTaskProfile profile = new CotTaskProfile(identities(), tasks);

    CanonicalTask task = profile.accept("tak-a", event("REPOSITION", "PUSH"), "<event/>".getBytes(StandardCharsets.UTF_8));

    assertEquals(CanonicalTaskType.REPOSITION, task.getTaskType());
    assertEquals(CanonicalTaskState.PENDING, task.getState());
    assertEquals("alpha", task.getTwinId());
    assertEquals(8.0, task.getTargetPosition().getAltitudeMslMeters());
  }

  @Test
  void duplicateReposition_doesNotCreateSecondTask() throws Exception {
    CanonicalTaskRegistry tasks = new CanonicalTaskRegistry();
    CotTaskProfile profile = new CotTaskProfile(identities(), tasks);

    CanonicalTask first = profile.accept("tak-a", event("REPOSITION", "PUSH"), new byte[] {1});
    CanonicalTask duplicate = profile.accept("tak-a", event("REPOSITION", "PUSH"), new byte[] {1});

    assertEquals(first.getCanonicalTaskId(), duplicate.getCanonicalTaskId());
    assertEquals(1, tasks.list().size());
  }

  @Test
  void unsupportedTask_isPersistedAsExplicitRejection() throws Exception {
    CotTaskProfile profile = new CotTaskProfile(identities(), new CanonicalTaskRegistry());

    CanonicalTask task = profile.accept("tak-a", event("LOITER", "PUSH"), new byte[] {1});

    assertEquals(CanonicalTaskState.REJECTED, task.getState());
    assertEquals(1, task.getTranslationWarnings().size());
  }

  @Test
  void repositionWithoutTarget_isPersistedAsExplicitRejection() throws Exception {
    CotTaskProfile profile = new CotTaskProfile(identities(), new CanonicalTaskRegistry());
    TakEvent event = event("REPOSITION", "PUSH");
    event.setPoint(null);

    CanonicalTask task = profile.accept("tak-a", event, new byte[] {1});

    assertEquals(CanonicalTaskState.REJECTED, task.getState());
    assertEquals("REPOSITION requires a target point", task.getResultReason());
  }

  @Test
  void cancellation_correlatesWithOriginalTask() throws Exception {
    CanonicalTaskRegistry tasks = new CanonicalTaskRegistry();
    CotTaskProfile profile = new CotTaskProfile(identities(), tasks);
    CanonicalTask original = profile.accept("tak-a", event("REPOSITION", "PUSH"), new byte[] {1});
    TakEvent cancellation = event("REPOSITION", "CANCEL");
    cancellation.getDetail().getTask().setTaskId("cancel-1");
    cancellation.getDetail().getTask().setOriginalTaskId("cot-task-1");

    CanonicalTask cancelled = profile.accept("tak-a", cancellation, new byte[] {2});

    assertEquals(original.getCanonicalTaskId(), cancelled.getCanonicalTaskId());
    assertEquals(CanonicalTaskState.PREEMPTING, cancelled.getState());
  }

  @Test
  void unknownSubject_isRejectedBeforeTaskCreation() {
    CotTaskProfile profile = new CotTaskProfile(identities(), new CanonicalTaskRegistry());
    TakEvent event = event("REPOSITION", "PUSH");
    event.getDetail().getTask().setSubjectUid("unknown");

    assertThrows(IllegalArgumentException.class, () -> profile.accept("tak-a", event, new byte[] {1}));
  }

  @Test
  void stanagCanonicalReposition_mapsToNamedCotTask() {
    CotIdentityRegistry identities = identities();
    CotTaskProfile profile = new CotTaskProfile(identities, new CanonicalTaskRegistry());
    CanonicalTask task = new CanonicalTask();
    task.setCanonicalTaskId(UUID.randomUUID());
    task.setOriginProtocol("stanag");
    task.getProtocolTaskIds().put("stanag", "4817-task-1");
    task.setTwinId("alpha");
    task.setTaskType(CanonicalTaskType.REPOSITION);
    task.setAction(CanonicalTaskAction.PUSH);
    task.setState(CanonicalTaskState.PENDING);
    task.setTargetPosition(new GeoPosition(47.1, 8.2, 8.0, null));

    TakEvent event = profile.map(task, identities.findByTwinId("alpha").orElseThrow(), Instant.parse("2026-09-08T10:00:00Z"));

    assertEquals("t-x-maps-task", event.getType());
    assertEquals("maps-stanag-4817-v1", event.getDetail().getTask().getProfile());
    assertEquals(38.0, event.getPoint().getHae());
  }

  private CotIdentityRegistry identities() {
    CotManagedPlatformConfigDTO platform = new CotManagedPlatformConfigDTO();
    platform.setEndpoint("tak-a");
    platform.setUid("vehicle-1");
    platform.setTwinId("alpha");
    platform.setOutboundUid("alpha-cot");
    platform.setTaskingProfile(CotTaskProfile.PROFILE_NAME);
    platform.setHaeToMslOffsetMeters(-30.0);
    CotTwinConfigDTO config = new CotTwinConfigDTO();
    config.setManagedPlatforms(List.of(platform));
    return new CotIdentityRegistry(config);
  }

  private TakEvent event(String taskType, String action) {
    TakEvent event = new TakEvent();
    event.setUid("task-event-1");
    event.setType("t-x-maps-task");
    event.setHow("m-g");
    event.setTime("2026-09-08T10:00:00Z");
    event.setStart("2026-09-08T10:00:00Z");
    event.setStale("2026-09-08T10:02:00Z");
    TakPoint point = new TakPoint();
    point.setLat(47.1);
    point.setLon(8.2);
    point.setHae(38.0);
    event.setPoint(point);
    TakTask task = new TakTask();
    task.setProfile(CotTaskProfile.PROFILE_NAME);
    task.setTaskId("cot-task-1");
    task.setSubjectUid("vehicle-1");
    task.setAction(action);
    task.setTaskType(taskType);
    TakDetail detail = new TakDetail();
    detail.setTask(task);
    event.setDetail(detail);
    return event;
  }
}
