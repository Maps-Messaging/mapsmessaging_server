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

package io.mapsmessaging.state.stanag.tasks.monitor;

import io.mapsmessaging.state.config.capability.Authorities;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.stanag.audit.AuditEvent;
import io.mapsmessaging.state.stanag.messages.feedback.TaskFeedbackDetails;
import io.mapsmessaging.state.util.GeoUtils;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
public class RepositionTaskMonitor extends TaskMonitor {

  public static final String TASK_TYPE = "REPOSITION";

  private static final double COMPLETE_DISTANCE_METERS = 10.0d;
  private static final double MINIMUM_CLOSING_SPEED_METERS_PER_SECOND = 0.1d;
  private static final double MINIMUM_PROGRESS_PERCENTAGE_FOR_ETA = 1.0d;
  private static final long MINIMUM_ELAPSED_SECONDS_FOR_ETA = 5L;

  private final GeoPosition targetPosition;

  private GeoPosition startPosition;
  private Instant startTimestamp;

  private double originalDistanceMeters;
  private double currentDistanceMeters;
  private double averageClosingSpeedMetersPerSecond;
  private double progressPercentage;
  private long estimatedSecondsToTarget;

  public RepositionTaskMonitor(
      UUID taskId,
      DroneTwin droneTwin,
      int taskSequence,
      Duration timeout,
      Duration feedbackInterval,
      GeoPosition targetPosition,
      AuditEvent auditEvent) {
    super(taskId, droneTwin, taskSequence, timeout, feedbackInterval, auditEvent);
    this.targetPosition = targetPosition;
    this.originalDistanceMeters = Double.MAX_VALUE;
    this.currentDistanceMeters = Double.MAX_VALUE;
    this.averageClosingSpeedMetersPerSecond = 0.0d;
    this.progressPercentage = 0.0d;
    this.estimatedSecondsToTarget = -1L;
  }

  @Override
  public String getTaskType() {
    return TASK_TYPE;
  }

  @Override
  protected void updateTask(DroneTwin droneTwin) {
    if (droneTwin == null || !getDroneUUID().equals(droneTwin.getUuid())) {
      return;
    }

    GeoPosition currentPosition = droneTwin.getGeoPosition();
    if (!hasUsablePosition(currentPosition) || !hasUsablePosition(targetPosition)) {
      return;
    }

    Instant currentTimestamp = Instant.now();

    if (startPosition == null) {
      initialisePosition(currentPosition, currentTimestamp);
      setInProgress();
      return;
    }

    updatePosition(currentPosition, currentTimestamp);

    if (currentDistanceMeters <= COMPLETE_DISTANCE_METERS) {
      progressPercentage = 100.0d;
      estimatedSecondsToTarget = 0L;
      setComplete();
      return;
    }

    setInProgress();
  }

  @Override
  protected TaskFeedbackDetails buildFeedbackDetails() {
    String timeRemaining = null;
    if (estimatedSecondsToTarget >= 0L) {
      timeRemaining = buildTimeRemaining();
    }
    return new TaskFeedbackDetails(progressPercentage, timeRemaining, null);
  }

  private void initialisePosition(GeoPosition currentPosition, Instant currentTimestamp) {
    startPosition = currentPosition;
    startTimestamp = currentTimestamp;

    originalDistanceMeters = GeoUtils.distanceMeters(currentPosition, targetPosition);
    currentDistanceMeters = originalDistanceMeters;
    averageClosingSpeedMetersPerSecond = 0.0d;
    progressPercentage = calculateProgressPercentage();
    estimatedSecondsToTarget = -1L;
  }

  private void updatePosition(GeoPosition currentPosition, Instant currentTimestamp) {
    currentDistanceMeters = GeoUtils.distanceMeters(currentPosition, targetPosition);
    updateProgress();
    updateAverageClosingSpeed(currentTimestamp);
    updateEstimatedSecondsToTarget(currentTimestamp);
  }

  private void updateProgress() {
    progressPercentage = calculateProgressPercentage();
  }

  private void updateAverageClosingSpeed(Instant currentTimestamp) {
    long elapsedSeconds = Duration.between(startTimestamp, currentTimestamp).toSeconds();

    if (elapsedSeconds <= 0L) {
      averageClosingSpeedMetersPerSecond = 0.0d;
      return;
    }

    double closedDistanceMeters = originalDistanceMeters - currentDistanceMeters;

    if (closedDistanceMeters <= 0.0d) {
      averageClosingSpeedMetersPerSecond = 0.0d;
      return;
    }

    averageClosingSpeedMetersPerSecond = closedDistanceMeters / elapsedSeconds;
  }

  private double calculateProgressPercentage() {
    if (originalDistanceMeters <= 0.0d) {
      return 100.0d;
    }
    double progress = ((originalDistanceMeters - currentDistanceMeters) / originalDistanceMeters) * 100.0d;
    return roundToOneDecimalPlace(Math.clamp(progress, 0.0d, 100.0d));
  }

  private void updateEstimatedSecondsToTarget(Instant currentTimestamp) {
    long elapsedSeconds = Duration.between(startTimestamp, currentTimestamp).toSeconds();
    if (elapsedSeconds < MINIMUM_ELAPSED_SECONDS_FOR_ETA) {
      estimatedSecondsToTarget = -1L;
      return;
    }
    if (progressPercentage < MINIMUM_PROGRESS_PERCENTAGE_FOR_ETA) {
      estimatedSecondsToTarget = -1L;
      return;
    }
    if (averageClosingSpeedMetersPerSecond <= MINIMUM_CLOSING_SPEED_METERS_PER_SECOND) {
      estimatedSecondsToTarget = -1L;
      return;
    }
    estimatedSecondsToTarget = Math.round(currentDistanceMeters / averageClosingSpeedMetersPerSecond);
  }

  private String buildTimeRemaining() {
    long secondsToTarget = Math.max(0L, Math.round(estimatedSecondsToTarget));
    return Duration.ofSeconds(secondsToTarget).toString();
  }

  private boolean hasUsablePosition(GeoPosition position) {
    return position != null
        && position.getLatitude() != null
        && position.getLongitude() != null;
  }

  private double roundToOneDecimalPlace(double value) {
    return Math.round(value * 10.0d) / 10.0d;
  }
}