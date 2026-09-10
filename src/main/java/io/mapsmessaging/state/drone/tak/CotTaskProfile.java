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

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakDetail;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.state.drone.tak.model.TakPoint;
import io.mapsmessaging.state.drone.tak.model.TakTask;
import io.mapsmessaging.state.drone.tak.model.TakTaskStatus;
import io.mapsmessaging.state.task.CanonicalTask;
import io.mapsmessaging.state.task.CanonicalTaskAction;
import io.mapsmessaging.state.task.CanonicalTaskRegistry;
import io.mapsmessaging.state.task.CanonicalTaskState;
import io.mapsmessaging.state.task.CanonicalTaskType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CotTaskProfile {

  public static final String PROFILE_NAME = "maps-stanag-4817-v1";
  private static final List<CanonicalTaskType> SUPPORTED = List.of(CanonicalTaskType.REPOSITION);

  private final CotIdentityRegistry identities;
  private final CanonicalTaskRegistry tasks;

  public CotTaskProfile(CotIdentityRegistry identities, CanonicalTaskRegistry tasks) {
    this.identities = identities;
    this.tasks = tasks;
  }

  public boolean isTaskEvent(TakEvent event) {
    return event != null && event.getDetail() != null && event.getDetail().getTask() != null;
  }

  public boolean isTaskStatusEvent(TakEvent event) {
    return event != null && event.getDetail() != null && event.getDetail().getTaskStatus() != null;
  }

  public CanonicalTask acceptStatus(TakEvent event) throws IOException {
    TakTaskStatus status = event.getDetail().getTaskStatus();
    CanonicalTask task = tasks.find("cot", required(status.getTaskId(), "taskId"))
        .orElseThrow(() -> new IllegalArgumentException("Unknown CoT task " + status.getTaskId()));
    CotIdentityBinding binding = identities.findByTwinId(task.getTwinId())
        .orElseThrow(() -> new IllegalArgumentException("CoT task subject is no longer configured"));
    if (!binding.taskingProfile().equals(status.getProfile())) {
      throw new IllegalArgumentException("Unsupported CoT tasking profile " + status.getProfile());
    }
    CanonicalTaskState state = enumValue(CanonicalTaskState.class, status.getState(), "task state");
    return tasks.updateState(
        task.getCanonicalTaskId(), state, status.getReason(), "cot").orElseThrow();
  }

  public CanonicalTask accept(String endpoint, TakEvent event, byte[] originalPayload) throws IOException {
    TakTask source = event.getDetail().getTask();
    CotIdentityBinding binding = identities.resolve(endpoint, source.getSubjectUid())
        .orElseThrow(() -> new IllegalArgumentException("CoT task subject is not a configured managed platform"));
    if (!binding.taskable()) {
      throw new IllegalArgumentException("CoT task subject is not taskable");
    }
    if (!binding.taskingProfile().equals(source.getProfile())) {
      throw new IllegalArgumentException("Unsupported CoT tasking profile " + source.getProfile());
    }

    CanonicalTaskAction action = enumValue(CanonicalTaskAction.class, source.getAction(), "task action");
    if (action == CanonicalTaskAction.CANCEL) {
      return cancel(source);
    }

    CanonicalTask task = new CanonicalTask();
    task.setOriginProtocol("cot");
    task.setRequester(source.getRequester());
    task.getProtocolTaskIds().put("cot", required(source.getTaskId(), "taskId"));
    task.setTwinId(binding.twinId());
    task.setTaskType(enumValue(CanonicalTaskType.class, source.getTaskType(), "task type"));
    task.setAction(action);
    task.setStartTime(Instant.parse(event.getStart()));
    task.setEndTime(source.getEnd() == null ? null : Instant.parse(source.getEnd()));
    task.setSpeedMetersPerSecond(source.getSpeed());
    task.setArrivalToleranceMeters(source.getArrivalTolerance());
    task.setResponseDestination(endpoint);
    task.setOriginalPayloadBase64(Base64.getEncoder().encodeToString(originalPayload));
    task.setTargetPosition(target(event.getPoint(), binding));
    if (event.getPoint() != null
        && event.getPoint().getHae() != null
        && binding.haeToMslOffsetMeters() == null) {
      task.getTranslationWarnings().add(
          "height above ellipsoid omitted because haeToMslOffsetMeters is not configured");
    }
    if (!SUPPORTED.contains(task.getTaskType())) {
      task.setState(CanonicalTaskState.REJECTED);
      task.setResultReason("Unsupported task type in " + PROFILE_NAME);
      task.getTranslationWarnings().add(task.getResultReason());
    } else {
      String validationFailure = validateReposition(task);
      if (validationFailure != null) {
        task.setState(CanonicalTaskState.REJECTED);
        task.setResultReason(validationFailure);
        task.getTranslationWarnings().add(validationFailure);
      }
    }
    return tasks.submit(task);
  }

  public TakEvent map(CanonicalTask task, CotIdentityBinding binding, Instant now) {
    Instant eventTime = now == null ? Instant.now() : now;
    TakEvent event = new TakEvent();
    event.setUid("maps-task-" + task.getCanonicalTaskId());
    event.setType(typeFor(task));
    event.setHow("m-g");
    event.setTime(eventTime.toString());
    event.setStart(eventTime.toString());
    event.setStale(eventTime.plus(120, ChronoUnit.SECONDS).toString());
    if (task.getTargetPosition() != null) {
      event.setPoint(point(task.getTargetPosition(), binding));
    }
    TakDetail detail = new TakDetail();
    if (!"cot".equalsIgnoreCase(task.getOriginProtocol())
        && task.getState() == CanonicalTaskState.PENDING
        && task.getAction() == CanonicalTaskAction.PUSH) {
      detail.setTask(task(task, binding));
    } else {
      detail.setTaskStatus(status(task, binding));
    }
    event.setDetail(detail);
    return event;
  }

  private CanonicalTask cancel(TakTask source) throws IOException {
    String originalTaskId = required(source.getOriginalTaskId(), "originalTaskId");
    CanonicalTask original = tasks.find("cot", originalTaskId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown CoT task " + originalTaskId));
    return tasks.updateState(
            original.getCanonicalTaskId(),
            CanonicalTaskState.PREEMPTING,
            "Cancellation requested",
            "cot")
        .orElseThrow();
  }

  private GeoPosition target(TakPoint point, CotIdentityBinding binding) {
    if (point == null || point.getLat() == null || point.getLon() == null) {
      return null;
    }
    Double altitudeMsl = null;
    if (point.getHae() != null && binding.haeToMslOffsetMeters() != null) {
      altitudeMsl = point.getHae() + binding.haeToMslOffsetMeters();
    }
    return new GeoPosition(point.getLat(), point.getLon(), altitudeMsl, null);
  }

  private TakPoint point(GeoPosition position, CotIdentityBinding binding) {
    TakPoint point = new TakPoint();
    point.setLat(position.getLatitude());
    point.setLon(position.getLongitude());
    if (position.getAltitudeMslMeters() != null && binding.haeToMslOffsetMeters() != null) {
      point.setHae(position.getAltitudeMslMeters() - binding.haeToMslOffsetMeters());
    }
    point.setCe(9999999.0);
    point.setLe(9999999.0);
    return point;
  }

  private TakTask task(CanonicalTask task, CotIdentityBinding binding) {
    TakTask result = new TakTask();
    result.setProfile(binding.taskingProfile());
    result.setTaskId(protocolId(task));
    result.setSubjectUid(binding.outboundUid());
    result.setAction(task.getAction().name());
    result.setTaskType(task.getTaskType().name());
    result.setRequester(task.getRequester());
    result.setEnd(task.getEndTime() == null ? null : task.getEndTime().toString());
    result.setSpeed(task.getSpeedMetersPerSecond());
    result.setArrivalTolerance(task.getArrivalToleranceMeters());
    return result;
  }

  private TakTaskStatus status(CanonicalTask task, CotIdentityBinding binding) {
    TakTaskStatus result = new TakTaskStatus();
    result.setProfile(binding.taskingProfile());
    result.setTaskId(protocolId(task));
    result.setState(task.getState().name());
    result.setReason(sanitiseReason(task.getResultReason()));
    return result;
  }

  private String protocolId(CanonicalTask task) {
    return task.getProtocolTaskIds().getOrDefault("cot", task.getCanonicalTaskId().toString());
  }

  private String typeFor(CanonicalTask task) {
    if (!"cot".equalsIgnoreCase(task.getOriginProtocol())
        && task.getState() == CanonicalTaskState.PENDING
        && task.getAction() == CanonicalTaskAction.PUSH) {
      return "t-x-maps-task";
    }
    return switch (task.getState()) {
      case PENDING -> "y-a-r";
      case ACTIVE -> "y-s-e";
      case COMPLETED -> "y-c-s";
      case REJECTED -> "y-c-f-r";
      case ABORTED, LOST -> "y-c-f-x";
      case PREEMPTING, PREEMPTED -> "y-c-f-x";
    };
  }

  private String validateReposition(CanonicalTask task) {
    if (task.getTargetPosition() == null
        || task.getTargetPosition().getLatitude() == null
        || task.getTargetPosition().getLongitude() == null) {
      return "REPOSITION requires a target point";
    }
    return null;
  }

  private String sanitiseReason(String reason) {
    if (reason == null) {
      return null;
    }
    String sanitised = reason.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
    return sanitised.substring(0, Math.min(256, sanitised.length()));
  }

  private String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("CoT task requires " + name);
    }
    return value;
  }

  private <T extends Enum<T>> T enumValue(Class<T> type, String value, String name) {
    try {
      return Enum.valueOf(type, required(value, name).toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Unsupported CoT " + name + " " + value, exception);
    }
  }
}
