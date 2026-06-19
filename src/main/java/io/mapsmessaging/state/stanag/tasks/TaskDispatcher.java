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

package io.mapsmessaging.state.stanag.tasks;

import io.mapsmessaging.state.config.capability.Authorities;
import io.mapsmessaging.state.config.capability.PlanTaskType;
import io.mapsmessaging.state.config.capability.TaskCapabilities;
import io.mapsmessaging.state.config.capability.TaskCapability;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.stanag.StanagSession;
import io.mapsmessaging.state.stanag.TaskAdminCommand;
import io.mapsmessaging.state.stanag.messages.result.ResultReason;
import io.mapsmessaging.state.stanag.tasks.monitor.TaskMonitor;

import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskDispatcher {

  private final StanagSession protocol;
  private final AtomicInteger taskSequence;
  private final Map<String, TaskHandler> taskHandlers = new ConcurrentHashMap<>();

  public TaskDispatcher(StanagSession protocol, AtomicInteger taskSequence) {
    this.protocol = protocol;
    this.taskSequence = taskSequence;
    ServiceLoader.load(TaskHandler.class).forEach(this::registerTaskHandler);
  }

  public TaskDispatchResult dispatch(EntityTwin twin, TaskAdminCommand command) {
    if (!(twin instanceof DroneTwin droneTwin)) {
      return TaskDispatchResult.rejected(null, ResultReason.CAPABILITY, "Matching twin is not a drone");
    }

    ResultReason authorisationRejectReason = getAuthorisationRejectReason(droneTwin, command);
    if (authorisationRejectReason != null) {
      return TaskDispatchResult.rejected(droneTwin, authorisationRejectReason, "Task is not authorised");
    }

    ResultReason validationRejectReason = getValidationRejectReason(droneTwin);
    if (validationRejectReason != null) {
      return TaskDispatchResult.rejected(droneTwin, validationRejectReason, "Drone is not armed");
    }

    TaskHandler taskHandler = taskHandlers.get(normaliseTaskType(command.getTaskType()));
    if (taskHandler == null) {
      return TaskDispatchResult.rejected(droneTwin, ResultReason.CAPABILITY, "No task handler for task type");
    }

    TaskMonitor taskMonitor = taskHandler.handle(droneTwin, command, protocol, taskSequence.getAndIncrement());
    Authorities authority = new Authorities();
    authority.setGuid(command.getAuthorityGuid());
    taskMonitor.setAuthority(authority);
    taskMonitor.setAccepted();
    return TaskDispatchResult.accepted(droneTwin, taskMonitor);
  }

  private void registerTaskHandler(TaskHandler taskHandler) {
    String taskType = normaliseTaskType(taskHandler.getTaskType());
    TaskHandler previousTaskHandler = taskHandlers.putIfAbsent(taskType, taskHandler);

    if (previousTaskHandler != null) {
      throw new IllegalStateException(
          "Duplicate task handler for task type "
              + taskType
              + ": "
              + previousTaskHandler.getClass().getName()
              + " and "
              + taskHandler.getClass().getName());
    }
  }

  private ResultReason getAuthorisationRejectReason(DroneTwin droneTwin, TaskAdminCommand command) {
    TaskCapabilities capabilities = droneTwin.getCapabilities();

    if (capabilities == null || capabilities.getTasks() == null) {
      return ResultReason.CAPABILITY;
    }

    TaskCapability capability =
        capabilities.getTasks().stream()
            .filter(taskCapability -> matchesTaskType(taskCapability.getTaskType(), command.getTaskType()))
            .findFirst()
            .orElse(null);

    if (capability == null) {
      return ResultReason.CAPABILITY;
    }

    if (capability.getAuthorities() == null || capability.getAuthorities().length == 0) {
      return null;
    }

    for (Authorities authority : capability.getAuthorities()) {
      if (authority.getGuid().equals(command.getAuthorityGuid())) {
        return null;
      }
    }

    return ResultReason.AUTHORITY;
  }

  private ResultReason getValidationRejectReason(DroneTwin droneTwin) {
    if (!Boolean.TRUE.equals(droneTwin.getArmed())) {
      return ResultReason.SAFETY;
    }

    return null;
  }

  private boolean matchesTaskType(PlanTaskType taskType, String taskTypeName) {
    if (taskType == null) {
      return false;
    }

    return taskType.name().equals(taskTypeName) || taskType.toString().equals(taskTypeName);
  }

  private String normaliseTaskType(String taskType) {
    if (taskType == null) {
      return "";
    }

    return taskType.toUpperCase(Locale.ROOT);
  }
}