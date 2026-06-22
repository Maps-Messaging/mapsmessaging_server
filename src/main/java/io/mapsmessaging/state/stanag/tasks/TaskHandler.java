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

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandInt;
import io.mapsmessaging.state.stanag.StanagSession;
import io.mapsmessaging.state.stanag.TaskAdminCommand;
import io.mapsmessaging.state.stanag.audit.AuditEvent;
import io.mapsmessaging.state.stanag.audit.Auditor;
import io.mapsmessaging.state.stanag.tasks.monitor.TaskMonitor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class TaskHandler {

  public abstract TaskMonitor handle(DroneTwin droneTwin, TaskAdminCommand command, StanagSession protocol, String template, int taskSequence);

  public abstract String getTaskType();

  protected void auditAndDispatch(DroneTwin droneTwin, StanagSession protocol, MavlinkCommandInt mavlinkRequest, AuditEvent auditEvent) {
    Auditor auditor = protocol.getAuditor();
    if (auditor != null) {
      auditTranslated(auditor, auditEvent);
    }
    sendMavlinkRequest(droneTwin, mavlinkRequest, protocol);
    if (auditor != null) {
      auditDispatched(auditor, auditEvent);
    }
  }

  protected void sendMavlinkRequest(DroneTwin droneTwin, MavlinkCommandInt mavlinkRequest, StanagSession protocol) {
    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder
        .setOpaqueData(mavlinkRequest.toMavlinkJsonObject(255, 0).toString().getBytes(StandardCharsets.UTF_8))
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setCorrelationData(droneTwin.getUniqueOutboundIdentifier());

    protocol.respond(droneTwin.getResponseTopicName(), messageBuilder.build());
  }

  protected void auditTranslated(Auditor auditor, AuditEvent auditEvent) {
    try {
      auditor.auditStanagCommandTranslated(auditEvent);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to audit STANAG command translation", exception);
    }
  }

  protected void auditDispatched(Auditor auditor, AuditEvent auditEvent) {
    try {
      auditor.auditDroneCommandDispatched(auditEvent);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to audit MAVLink command dispatch", exception);
    }
  }
}