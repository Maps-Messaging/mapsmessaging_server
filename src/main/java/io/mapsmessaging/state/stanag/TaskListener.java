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

package io.mapsmessaging.state.stanag;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.config.StanagConfig;
import io.mapsmessaging.state.config.capability.Authorities;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.stanag.audit.AuditEvent;
import io.mapsmessaging.state.stanag.audit.Auditor;
import io.mapsmessaging.state.stanag.messages.*;
import io.mapsmessaging.state.stanag.messages.core.MessageHeaderBuilder;
import io.mapsmessaging.state.stanag.messages.task.admin.TaskAdminActionEnum;
import io.mapsmessaging.state.stanag.messages.task.admin.TaskAdminMessage;
import io.mapsmessaging.state.stanag.messages.task.feedback.TaskFeedbackMessage;
import io.mapsmessaging.state.stanag.messages.task.feedback.TaskFeedbackMessageBuilder;
import io.mapsmessaging.state.stanag.messages.task.result.ResultReason;
import io.mapsmessaging.state.stanag.messages.task.result.ResultReasonBuilder;
import io.mapsmessaging.state.stanag.messages.task.result.TaskResultMessage;
import io.mapsmessaging.state.stanag.messages.task.result.TaskResultMessageBuilder;
import io.mapsmessaging.state.stanag.tasks.TaskDispatchResult;
import io.mapsmessaging.state.stanag.tasks.TaskDispatcher;
import io.mapsmessaging.state.stanag.tasks.monitor.TaskMonitor;
import io.mapsmessaging.state.stanag.tasks.monitor.TaskMonitorManager;
import io.mapsmessaging.state.stanag.tasks.monitor.TaskStatusPublisher;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TaskListener implements Consumer<JsonObject>, TaskStatusPublisher {

  private final TwinManager twinManager;
  private final Auditor auditor;
  private final TaskDispatcher taskDispatcher;
  private final TaskResultMessageBuilder taskResultMessageBuilder;
  private final TaskEventPublisher taskEventPublisher;
  private final TaskMonitorManager taskMonitorManager;
  private final TaskFeedbackMessageBuilder taskFeedbackMessageBuilder;

  public TaskListener(TwinManager twinManager, StanagSession protocol, StanagConfig stanagConfig) {
    this.twinManager = twinManager;
    this.auditor = twinManager.getAuditor();
    this.taskDispatcher = new TaskDispatcher(protocol, stanagConfig, new AtomicInteger(0));
    this.taskResultMessageBuilder = new TaskResultMessageBuilder(new MessageHeaderBuilder(Clock.systemUTC()), new ResultReasonBuilder());
    this.taskFeedbackMessageBuilder = new TaskFeedbackMessageBuilder(new MessageHeaderBuilder(Clock.systemUTC()));
    this.taskEventPublisher = new TaskEventPublisher(protocol, new TaskSchemaValidator(), stanagConfig.getTaskTopicTemplate());
    this.taskMonitorManager = new TaskMonitorManager(Duration.ofSeconds(5), this);
    twinManager.addObserver(taskMonitorManager);
  }

  public void start() {
    taskMonitorManager.start();
  }

  public void stop() {
    taskMonitorManager.close();
  }

  @Override
  public void publishFeedback(TaskMonitor taskMonitor, TaskFeedbackMessage taskFeedbackMessage) {
    taskEventPublisher.publishFeedback(taskMonitor.getDroneUUID(), taskFeedbackMessage);
  }

  @Override
  public void publishResult(TaskMonitor taskMonitor, TaskResultMessage taskResultMessage) {
    taskEventPublisher.publishResult(taskMonitor.getDroneUUID(), taskResultMessage);
    auditTaskResult(taskMonitor, taskResultMessage);
  }

  public void publishAdmin(TaskMonitor taskMonitor, TaskAdminMessage taskAdminMessage) {
    taskEventPublisher.publishAdmin(taskMonitor.getDroneUUID(), taskAdminMessage);
    auditTaskAdmin(taskMonitor, taskAdminMessage);
  }

  @Override
  public void accept(JsonObject jsonObject) {
    TaskAdminCommand command = createTaskCommand(jsonObject);
    if (command != null) {
      if(command.getAction().equals(TaskAdminActionEnum.PUSH.name())) {
        DroneTwin droneTwin = findTwin(command);
        if (droneTwin == null) {
          return;
        }
        handleTaskForTwin(droneTwin, command);
      }
    } else {
      // log this
    }
  }

  private void handleTaskForTwin(DroneTwin droneTwin, TaskAdminCommand command) {
    TaskDispatchResult dispatchResult = taskDispatcher.dispatch(droneTwin, command);
    if (!dispatchResult.isAccepted()) {
      rejectTask(dispatchResult.getDroneTwin(), command, dispatchResult.getRejectReason(), dispatchResult.getRejectText());
    }
    else{
    TaskMonitor taskMonitor = dispatchResult.getTaskMonitor();
    if (taskMonitor != null) {
      acceptTask(taskMonitor, command);
      taskMonitorManager.add(taskMonitor);
    }
    }
  }

  private void acceptTask(TaskMonitor taskStat, TaskAdminCommand command) {
    Authorities authority = new Authorities(command.getAuthorityGuid());
    TaskStatusContext context = new TaskStatusContext(command.getTaskId(), command.getNodeIdentifier(), authority);
    taskEventPublisher.publishFeedback(taskStat.getDroneUUID(), taskFeedbackMessageBuilder.build(context, TaskState.ACTIVE));
  }

  private void rejectTask(DroneTwin droneTwin, TaskAdminCommand command, ResultReason resultReason, String reasonText) {
    TaskResultMessage taskResultMessage = buildRejectedTaskResult(command, resultReason, reasonText);
    taskEventPublisher.publishResult(droneTwin.getUuid(), taskResultMessage);
    auditRejected(droneTwin, command, resultReason, reasonText);
  }

  private void rejectWithoutTwin(TaskAdminCommand command, ResultReason resultReason, String reasonText) {
    TaskResultMessage taskResultMessage = buildRejectedTaskResult(command, resultReason, reasonText);
    auditRejected(null, command, resultReason, reasonText);
  }

  private TaskResultMessage buildRejectedTaskResult(TaskAdminCommand command, ResultReason resultReason, String reasonText) {
    Authorities authority = new Authorities(command.getAuthorityGuid());
    TaskStatusContext context = new TaskStatusContext(command.getTaskId(), command.getNodeIdentifier(), authority);
    return taskResultMessageBuilder.buildRejected(context, resultReason, reasonText);
  }

  private void auditRejected(DroneTwin droneTwin, TaskAdminCommand command, ResultReason resultReason, String reasonText) {
    if (auditor != null) {
      try {
        auditor.auditStanagCommandRejected(buildRejectedAuditEvent(droneTwin, command), resultReason.name() + ": " + reasonText);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
  }

  private void auditTaskAdmin(TaskMonitor taskMonitor, TaskAdminMessage taskAdminMessage) {
    if (auditor != null) {
      AuditEvent auditEvent = taskMonitor.getAuditEvent();
      if (auditEvent == null) {
        throw new IllegalStateException("Task monitor has no audit event for task " + taskMonitor.getTaskId());
      }

      try {
        if (isSuccessfulTaskAdmin(taskAdminMessage)) {
          auditor.auditStanagTaskResultPublished(auditEvent);
        } else {
          auditor.auditStanagTaskResultFailed(auditEvent, taskAdminMessage.getBody().getAction().name());
        }
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
  }

  private void auditTaskResult(TaskMonitor taskMonitor, TaskResultMessage taskResultMessage) {
    if (auditor != null) {
      AuditEvent auditEvent = taskMonitor.getAuditEvent();
      if (auditEvent == null) {
        throw new IllegalStateException("Task monitor has no audit event for task " + taskMonitor.getTaskId());
      }
      try {
        if (isSuccessfulTaskResult(taskResultMessage)) {
          auditor.auditStanagTaskResultPublished(auditEvent);
        } else {
          auditor.auditStanagTaskResultFailed(auditEvent, taskResultMessage.getBody().getState().name());
        }
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
  }

  private AuditEvent buildRejectedAuditEvent(DroneTwin droneTwin, TaskAdminCommand command) {
    UUID nodeIdentifier = command.getNodeIdentifier();
    String droneId = nodeIdentifier.toString();
    String destination = droneTwin == null ? droneId : droneTwin.getTwinId();

    return AuditEvent.builder()
        .auditId(command.getIdentifier())
        .correlationId(command.getIdentifier())
        .parentCorrelationId("")
        .actor("stanag")
        .actorType("system")
        .source("stanag")
        .destination(destination)
        .subject(droneId)
        .taskId(command.getIdentifier())
        .commandId(command.getIdentifier())
        .droneId(droneId)
        .stanagTaskType(command.getTaskType())
        .protocol("stanag-4817")
        .build();
  }

  private boolean isSuccessfulTaskResult(TaskResultMessage taskResultMessage) {
    return taskResultMessage.getBody().getState() == TaskState.SUCCEEDED;
  }

  private boolean isSuccessfulTaskAdmin(TaskAdminMessage taskAdminMessage) {
    return taskAdminMessage.getBody().getAction() == TaskAdminActionEnum.ASSIGN;
  }

  private void publishResult(TaskResultMessage taskResultMessage) {
    // Existing reject publishing behaviour preserved while you sort out the reject path.
  }


  private TaskAdminCommand createTaskCommand(JsonObject jsonObject) {
    try {
      return TaskAdminCommand.fromJson(jsonObject);
    } catch (TaskAdminCommandException e) {
      // Log this and respond
    }
    return null;
  }

  private DroneTwin findTwin(TaskAdminCommand command) {
    UUID nodeUuid;

    try {
      nodeUuid = command.getNodeIdentifier();
    } catch (IllegalArgumentException exception) {
      rejectWithoutTwin(command, ResultReason.LOGIC, "Invalid node identifier");
      return null;
    }

    if (nodeUuid == null) {
      rejectWithoutTwin(command, ResultReason.LOGIC, "Invalid node identifier");
      return null;
    }

    Optional<EntityTwin> matchingTwin =
        twinManager.listTwins().stream()
            .filter(entityTwin -> nodeUuid.equals(entityTwin.getUuid()))
            .findFirst();

    if (matchingTwin.isEmpty()) {
      rejectWithoutTwin(command, ResultReason.CAPABILITY, "No matching twin found for node");
      return null;
    }

    EntityTwin entityTwin = matchingTwin.get();
    if (!(entityTwin instanceof DroneTwin droneTwin)) {
      rejectWithoutTwin(command, ResultReason.CAPABILITY, "Matching twin is not a drone");
      return null;
    }

    return droneTwin;
  }

}