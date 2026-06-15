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
import io.mapsmessaging.state.config.capability.Authorities;
import io.mapsmessaging.state.config.capability.PlanTaskType;
import io.mapsmessaging.state.config.capability.TaskCapabilities;
import io.mapsmessaging.state.config.capability.TaskCapability;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.stanag.tasks.LoiterTaskHandler;
import io.mapsmessaging.state.stanag.tasks.PrepareTaskHandler;
import io.mapsmessaging.state.stanag.tasks.RepositionTaskHandler;
import io.mapsmessaging.state.stanag.tasks.TaskHandler;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TaskListener implements Consumer<JsonObject> {

  private final TwinManager twinManager;
  private final AtomicInteger taskSequence = new AtomicInteger(0);
  private final StanagStateSubscriber protocol;

  private final Map<String, TaskHandler> taskHandlers = new ConcurrentHashMap<>();

  public TaskListener(TwinManager twinManager, StanagStateSubscriber protocol) {
    this.twinManager = twinManager;
    this.protocol = protocol;
    ServiceLoader.load(TaskHandler.class).forEach(this::registerTaskHandler);
  }

  private void registerTaskHandler(TaskHandler taskHandler) {
    TaskHandler previousTaskHandler = taskHandlers.putIfAbsent(taskHandler.getTaskType(), taskHandler);
    if (previousTaskHandler != null) {
      throw new IllegalStateException("Duplicate task handler for task type " + taskHandler.getTaskType() + ": " + previousTaskHandler.getClass().getName() + " and " + taskHandler.getClass().getName());
    }
  }

  @Override
  public void accept(JsonObject jsonObject) {
    try {
      //audit receipt of task request
      TaskAdminCommand command = TaskAdminCommand.fromJson(jsonObject);
      String nodeId = command.getNodeIdentifier();
      UUID nodeUuid = UUID.fromString(nodeId);

      Optional<EntityTwin> matchingTwin = twinManager.listTwins()
          .stream()
          .filter(entityTwin -> nodeUuid.equals(entityTwin.getUuid()))
          .findFirst();

      if(matchingTwin.isEmpty()){
        //audit no such drone found
        System.out.println("No matching twin found for node " + nodeId);
      }
      else{
        handleTaskForTwin(matchingTwin.get(), command);
      }
    } catch (TaskAdminCommandException e) {
      e.printStackTrace();
      // Log and Audit this
      // send a rejected task
    }
  }

  private void handleTaskForTwin(EntityTwin twin, TaskAdminCommand command) {
    if(twin instanceof DroneTwin droneTwin && isAuthorised(droneTwin, command) && isValidState(droneTwin, command)){
      TaskHandler taskHandler = taskHandlers.get(command.getTaskType().toUpperCase());
      if(taskHandler != null){
        taskHandler.handle(droneTwin, command, protocol, taskSequence.getAndIncrement());
      }
    }
  }

  private boolean isAuthorised(DroneTwin droneTwin, TaskAdminCommand command){
    boolean authorised = false;
    TaskCapabilities capabilities = droneTwin.getCapabilities();
    TaskCapability capability = capabilities.getTasks()
        .stream()
        .filter(taskCapability -> matchesTaskType(taskCapability.getTaskType(), command.getTaskType()))
        .findFirst()
        .orElse(null);

    if (capability == null) {
      // Send Reject with reason
      System.out.println("No matching capability found for task " + command.getAction() + " " + command.getTaskType());
    } else {
      if (capability.getAuthorities() == null || capability.getAuthorities().length == 0) {
        authorised = true;
      } else {
        for (Authorities authority : capability.getAuthorities()) {
          if (authority.getGuid().equals(command.getAuthorityGuid())) {
            authorised = true;
            break;
          }
        }
      }
      if (!authorised) {
        // Send Reject with not authorised reason
        System.out.println("Not authorised to perform task " + command.getAction());
      }
    }
    return authorised;
  }

  private boolean isValidState(DroneTwin droneTwin, TaskAdminCommand command) {
    if (droneTwin == null) {
      // send reject with no supporting task type
      return false;
    }
    return true;
  }

  private boolean matchesTaskType(PlanTaskType taskType, String taskTypeName) {
    if (taskType == null) {
      return false;
    }
    return taskType.name().equals(taskTypeName) || taskType.toString().equals(taskTypeName);
  }
}
