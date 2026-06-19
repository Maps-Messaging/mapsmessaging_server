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
import io.mapsmessaging.state.stanag.audit.AuditEvent;
import io.mapsmessaging.state.stanag.messages.*;
import io.mapsmessaging.state.stanag.messages.feedback.TaskFeedbackDetails;
import io.mapsmessaging.state.stanag.messages.feedback.TaskFeedbackMessage;
import io.mapsmessaging.state.stanag.messages.feedback.TaskFeedbackMessageBuilder;
import io.mapsmessaging.state.stanag.messages.result.ResultReason;
import io.mapsmessaging.state.stanag.messages.result.TaskResultMessage;
import io.mapsmessaging.state.stanag.messages.result.TaskResultMessageBuilder;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static io.mapsmessaging.state.stanag.messages.TaskState.LOST;

@Getter
public abstract class TaskMonitor {

  private final UUID taskId;
  private final UUID droneUUID;
  private final int taskSequence;
  private final Duration timeout;
  private final Duration feedbackInterval;
  private final Instant createdTimestamp;
  private final AuditEvent auditEvent;
  private final String topicPath;

  private TaskMonitorState state;
  private Instant lastUpdatedTimestamp;
  private Instant lastFeedbackTimestamp;
  private boolean resultEmitted;


  @Setter
  private Authorities authority;

  protected TaskMonitor(
      UUID taskId,
      DroneTwin droneTwin,
      int taskSequence,
      Duration timeout,
      Duration feedbackInterval,
      AuditEvent auditEvent) {
    this.taskId = taskId;
    this.droneUUID = droneTwin.getUuid();
    this.taskSequence = taskSequence;
    this.timeout = timeout;
    this.feedbackInterval = feedbackInterval;
    this.auditEvent = auditEvent;
    this.createdTimestamp = Instant.now();
    this.lastUpdatedTimestamp = createdTimestamp;
    this.state = TaskMonitorState.ACCEPTED;
    this.topicPath = droneTwin.getTwinId();
  }

  public abstract String getTaskType();

  public final void update(DroneTwin droneTwin) {
    if (isFinished()) {
      return;
    }

    Instant now = Instant.now();
    if (timeout != null && Duration.between(createdTimestamp, now).compareTo(timeout) > 0) {
      setTimedOut();
      return;
    }

    updateTask(droneTwin);
    lastUpdatedTimestamp = now;
  }

  protected abstract void updateTask(DroneTwin droneTwin);

  public boolean hasFeedback() {
    if (isFinished()) {
      return false;
    }

    if (feedbackInterval == null) {
      return false;
    }

    if (lastFeedbackTimestamp == null) {
      return true;
    }

    return Duration.between(lastFeedbackTimestamp, Instant.now()).compareTo(feedbackInterval) >= 0;
  }

  public TaskFeedbackMessage buildFeedback(TaskFeedbackMessageBuilder feedbackMessageBuilder) {
    TaskFeedbackMessage feedbackMessage = feedbackMessageBuilder.build(buildStatusContext(), mapFeedbackState(), buildFeedbackDetails());
    markFeedbackEmitted();
    return feedbackMessage;
  }

  public boolean hasResult() {
    return isFinished() && !resultEmitted;
  }

  public TaskResultMessage buildResult(TaskResultMessageBuilder resultMessageBuilder) {
    TaskResultMessage resultMessage =
        resultMessageBuilder.build(buildStatusContext(), mapResultState(), buildResultReason());

    markResultEmitted();
    return resultMessage;
  }

  public boolean isFinished() {
    return state == TaskMonitorState.COMPLETE
        || state == TaskMonitorState.FAILED
        || state == TaskMonitorState.TIMEOUT
        || state == TaskMonitorState.LOST;
  }

  public void setAccepted() {
    if (!isFinished()) {
      state = TaskMonitorState.ACCEPTED;
    }
  }

  public void setInProgress() {
    if (!isFinished()) {
      state = TaskMonitorState.IN_PROGRESS;
    }
  }

  public void setComplete() {
    if (!isFinished()) {
      state = TaskMonitorState.COMPLETE;
    }
  }

  public void setFailed() {
    if (!isFinished()) {
      state = TaskMonitorState.FAILED;
    }
  }

  public void setTimedOut() {
    if (!isFinished()) {
      state = TaskMonitorState.TIMEOUT;
    }
  }

  public void setLost() {
    if (!isFinished()) {
      state = TaskMonitorState.LOST;
    }
  }

  protected TaskFeedbackDetails buildFeedbackDetails() {
    return null;
  }

  protected ResultReason buildResultReason() {
    return null;
  }

  private TaskStatusContext buildStatusContext() {
    return new TaskStatusContext(taskId, droneUUID, authority);
  }

  private TaskState mapFeedbackState() {
    return TaskState.ACTIVE;
  }

  private TaskState mapResultState() {
    return switch (state) {
      case COMPLETE -> TaskState.SUCCEEDED;
      case FAILED -> TaskState.ABORTED;
      case TIMEOUT, LOST -> LOST;
      case ACCEPTED, IN_PROGRESS -> TaskState.ACTIVE;
    };
  }

  private void markFeedbackEmitted() {
    lastFeedbackTimestamp = Instant.now();
  }

  private void markResultEmitted() {
    resultEmitted = true;
  }
}