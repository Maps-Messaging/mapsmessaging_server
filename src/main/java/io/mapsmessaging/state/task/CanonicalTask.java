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

import io.mapsmessaging.state.drone.model.GeoPosition;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class CanonicalTask {
  private UUID canonicalTaskId;
  private String originProtocol;
  private String requester;
  private Map<String, String> protocolTaskIds = new LinkedHashMap<>();
  private String twinId;
  private UUID twinUuid;
  private CanonicalTaskType taskType;
  private CanonicalTaskAction action = CanonicalTaskAction.PUSH;
  private GeoPosition targetPosition;
  private String targetEntityId;
  private Instant startTime;
  private Instant endTime;
  private Double speedMetersPerSecond;
  private Double arrivalToleranceMeters;
  private CanonicalTaskState state = CanonicalTaskState.PENDING;
  private String responseDestination;
  private String resultReason;
  private String lastUpdateProtocol;
  private String originalPayloadBase64;
  private List<String> translationWarnings = new ArrayList<>();

  public CanonicalTask copy() {
    CanonicalTask copy = new CanonicalTask();
    copy.canonicalTaskId = canonicalTaskId;
    copy.originProtocol = originProtocol;
    copy.requester = requester;
    copy.protocolTaskIds = new LinkedHashMap<>(protocolTaskIds);
    copy.twinId = twinId;
    copy.twinUuid = twinUuid;
    copy.taskType = taskType;
    copy.action = action;
    copy.targetPosition = targetPosition == null ? null : new GeoPosition(
        targetPosition.getLatitude(),
        targetPosition.getLongitude(),
        targetPosition.getAltitudeMslMeters(),
        targetPosition.getAltitudeAglMeters());
    copy.targetEntityId = targetEntityId;
    copy.startTime = startTime;
    copy.endTime = endTime;
    copy.speedMetersPerSecond = speedMetersPerSecond;
    copy.arrivalToleranceMeters = arrivalToleranceMeters;
    copy.state = state;
    copy.responseDestination = responseDestination;
    copy.resultReason = resultReason;
    copy.lastUpdateProtocol = lastUpdateProtocol;
    copy.originalPayloadBase64 = originalPayloadBase64;
    copy.translationWarnings = new ArrayList<>(translationWarnings);
    return copy;
  }
}
