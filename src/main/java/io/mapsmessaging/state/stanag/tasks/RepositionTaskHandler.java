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

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandInt;
import io.mapsmessaging.state.stanag.StanagSession;
import io.mapsmessaging.state.stanag.TaskAdminCommand;
import io.mapsmessaging.state.stanag.audit.AuditEvent;
import io.mapsmessaging.state.stanag.tasks.monitor.RepositionTaskMonitor;
import io.mapsmessaging.state.stanag.tasks.monitor.TaskMonitor;
import io.mapsmessaging.state.util.GeoUtils;

import java.time.Duration;
import java.util.UUID;

public class RepositionTaskHandler extends TaskHandler {

  @Override
  public TaskMonitor handle(DroneTwin droneTwin, TaskAdminCommand command, StanagSession protocol,String template, int taskSequence) {
    GeoPosition newPosition = command.getPosition();
    float yaw = GeoUtils.bearingDegrees(droneTwin.getGeoPosition(), newPosition);
    MavlinkCommandInt mavlinkRequest = MavlinkCommandInt.reposition(droneTwin.getSystemId(), droneTwin.getComponentId(), newPosition, yaw, taskSequence);
    AuditEvent auditEvent = buildAuditEvent(droneTwin, command, mavlinkRequest, taskSequence, newPosition, yaw);
    auditAndDispatch(droneTwin, protocol, mavlinkRequest, auditEvent);
    return new RepositionTaskMonitor(command.getTaskId(), droneTwin, template, taskSequence, Duration.ofMinutes(60), Duration.ofSeconds(15), command.getPosition(), auditEvent);
  }

  @Override
  public String getTaskType() {
    return "REPOSITION";
  }

  private AuditEvent buildAuditEvent(
      DroneTwin droneTwin,
      TaskAdminCommand command,
      MavlinkCommandInt mavlinkRequest,
      int taskSequence,
      GeoPosition newPosition,
      float yaw
  ) {
    String commandIdentifier = command.getIdentifier();
    String droneIdentifier = droneTwin.getUuid().toString();

    AuditEvent auditEvent = AuditEvent.builder()
        .auditId(UUID.randomUUID().toString())
        .correlationId(commandIdentifier)
        .actor("stanag")
        .actorType("system")
        .source("stanag")
        .destination("drone")
        .subject(droneIdentifier)
        .taskId(commandIdentifier)
        .commandId(commandIdentifier)
        .droneId(droneIdentifier)
        .stanagTaskType(getTaskType())
        .droneCommandType(mavlinkRequest.getMessageType())
        .protocol("MAVLink")
        .targetSystem(String.valueOf(droneTwin.getSystemId()))
        .targetComponent(String.valueOf(droneTwin.getComponentId()))
        .build();

    auditEvent.addAttribute("taskSequence", String.valueOf(taskSequence));
    auditEvent.addAttribute("mavlinkCommand", String.valueOf(mavlinkRequest.getCommand()));

    if (newPosition.getLatitude() != null) {
      auditEvent.addAttribute("latitude", String.valueOf(newPosition.getLatitude()));
    }

    if (newPosition.getLongitude() != null) {
      auditEvent.addAttribute("longitude", String.valueOf(newPosition.getLongitude()));
    }

    if (newPosition.getAltitudeMslMeters() != null) {
      auditEvent.addAttribute("altitudeMslMeters", String.valueOf(newPosition.getAltitudeMslMeters()));
    }

    if (newPosition.getAltitudeAglMeters() != null) {
      auditEvent.addAttribute("altitudeAglMeters", String.valueOf(newPosition.getAltitudeAglMeters()));
    }

    auditEvent.addAttribute("yaw", String.valueOf(yaw));

    return auditEvent;
  }
}