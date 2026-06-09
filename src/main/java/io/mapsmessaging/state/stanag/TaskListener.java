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
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.config.DroneInfo;
import io.mapsmessaging.state.config.capability.TaskCapabilities;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TaskListener implements Consumer<JsonObject> {

  private final TwinManager twinManager;
  private final AtomicInteger taskSequence = new AtomicInteger(0);

  private final StanagStateSubscriber protocol;

  public TaskListener(TwinManager twinManager, StanagStateSubscriber protocol) {
    this.twinManager = twinManager;
    this.protocol = protocol;
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
    }
  }

  private void handleTaskForTwin(EntityTwin twin, TaskAdminCommand command) {
    if(twin instanceof DroneTwin droneTwin){
      TaskCapabilities capabilities = droneTwin.getCapabilities();
      Map<String, Object> description = droneTwin.getDescription();

      // Need to validate that the request is valid and the drone is capable of it

      // Build mavlink goto request for drone
      GeoPosition geoPosition = command.getPosition();
      MavlinkCommandInt mavlinkRequest = MavlinkCommandInt.reposition(droneTwin.getSystemId(), droneTwin.getComponentId(), geoPosition, taskSequence.getAndIncrement());
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(mavlinkRequest.toMavlinkJsonObject(255, 0).toString().getBytes(StandardCharsets.UTF_8))
              .setQoS(QualityOfService.AT_MOST_ONCE)
          .setCorrelationData(twin.getUniqueOutboundIdentifier());
      protocol.respond(twin.getResponseTopicName(), messageBuilder.build());

    }
  }
}
