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

import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.stanag.messages.*;
import io.mapsmessaging.state.stanag.messages.feedback.TaskFeedbackMessage;
import io.mapsmessaging.state.stanag.messages.feedback.TaskFeedbackMessageBuilder;
import io.mapsmessaging.state.stanag.messages.result.ResultReasonBuilder;
import io.mapsmessaging.state.stanag.messages.result.TaskResultMessage;
import io.mapsmessaging.state.stanag.messages.result.TaskResultMessageBuilder;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskMonitorManager implements AutoCloseable, TwinObserver {

  private final Duration scanInterval;
  private final TaskStatusPublisher statusPublisher;

  private final TaskFeedbackMessageBuilder feedbackMessageBuilder;
  private final TaskResultMessageBuilder resultMessageBuilder;

  @Getter
  private final List<TaskMonitor> taskMonitors = new CopyOnWriteArrayList<>();

  private final AtomicBoolean running = new AtomicBoolean(false);

  public TaskMonitorManager(Duration scanInterval, TaskStatusPublisher statusPublisher) {
    this.scanInterval = scanInterval;
    this.statusPublisher = statusPublisher;

    Clock clock = Clock.systemUTC();
    MessageHeaderBuilder messageHeaderBuilder = new MessageHeaderBuilder(clock);
    feedbackMessageBuilder = new TaskFeedbackMessageBuilder(messageHeaderBuilder);
    ResultReasonBuilder resultReasonBuilder = new ResultReasonBuilder();
    resultMessageBuilder = new TaskResultMessageBuilder(messageHeaderBuilder, resultReasonBuilder);
  }

  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this::scanTaskMonitors, scanInterval.toMillis(), scanInterval.toMillis(), TimeUnit.MILLISECONDS);
  }

  public void add(TaskMonitor taskMonitor) {
    if (taskMonitor == null) {
      return;
    }
    taskMonitors.add(taskMonitor);
  }

  public void update(DroneTwin droneTwin) {
    if (droneTwin == null) {
      return;
    }

    for (TaskMonitor taskMonitor : taskMonitors) {
      if (taskMonitor.getDroneUUID().equals(droneTwin.getUuid())) {
        taskMonitor.update(droneTwin);
      }
    }
  }

  public void scanTaskMonitors() {
    if (!running.get()) {
      return;
    }

    for (TaskMonitor taskMonitor : taskMonitors) {
      scanTaskMonitor(taskMonitor);
    }
  }

  @Override
  public void close() {
    running.set(false);
    taskMonitors.clear();
  }

  private void scanTaskMonitor(TaskMonitor taskMonitor) {
    if (taskMonitor.hasResult()) {
      publishResult(taskMonitor);
      taskMonitors.remove(taskMonitor);
      return;
    }

    if (taskMonitor.hasFeedback()) {
      publishFeedback(taskMonitor);
    }
  }

  private void publishFeedback(TaskMonitor taskMonitor) {
    TaskFeedbackMessage taskFeedbackMessage = taskMonitor.buildFeedback(feedbackMessageBuilder);

    statusPublisher.publishFeedback(taskMonitor, taskFeedbackMessage);
  }

  private void publishResult(TaskMonitor taskMonitor) {
    TaskResultMessage taskResultMessage = taskMonitor.buildResult(resultMessageBuilder);

    statusPublisher.publishResult(taskMonitor, taskResultMessage);
  }


  @Override
  public void onTwinUpdated(String twinId, EntityTwin current, TwinUpdateContext context) {
    if (!(current instanceof DroneTwin droneTwin)) {
      return;
    }

    update(droneTwin);
  }

  @Override
  public void onTwinRemoved(EntityTwin removed, TwinUpdateContext context) {
    if (!(removed instanceof DroneTwin droneTwin)) {
      return;
    }

    cancelTaskMonitorsForDrone(droneTwin.getUuid());
  }

  private void cancelTaskMonitorsForDrone(UUID droneId) {
    for (TaskMonitor taskMonitor : taskMonitors) {
      if (!taskMonitor.getDroneUUID().equals(droneId)) {
        continue;
      }

      taskMonitor.setLost();

      if (taskMonitor.hasResult()) {
        publishResult(taskMonitor);
      }

      taskMonitors.remove(taskMonitor);
    }
  }
}