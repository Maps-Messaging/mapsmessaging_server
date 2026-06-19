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

package io.mapsmessaging.state.stanag.audit;

import io.mapsmessaging.audit.*;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Auditor {

  private final AuditLogger auditLogger;
  private final AuditPayloadStore auditPayloadStore;

  public Auditor(AuditLogger auditLogger, AuditPayloadStore auditPayloadStore) {
    this.auditLogger = auditLogger;
    this.auditPayloadStore = auditPayloadStore;
  }

  public AuditRecord auditStanagCommandReceived(AuditEvent auditEvent, AuditPayload... payloads)
      throws IOException {
    return auditLogger.audit(
        AuditMessages.STANAG_COMMAND_RECEIVED,
        buildAuditContext(auditEvent, "stanag-command-received", AuditOutcome.SUCCESS),
        writePayloads(auditEvent, payloads),
        safe(auditEvent.getStanagTaskType()),
        safe(auditEvent.getDroneId()),
        safe(auditEvent.getCommandId()));
  }

  public AuditRecord auditStanagCommandRejected(
      AuditEvent auditEvent, String reason, AuditPayload... payloads) throws IOException {
    return auditLogger.audit(
        AuditMessages.STANAG_COMMAND_REJECTED,
        buildAuditContext(auditEvent, "stanag-command-rejected", AuditOutcome.REJECTED),
        writePayloads(auditEvent, payloads),
        safe(auditEvent.getStanagTaskType()),
        safe(auditEvent.getDroneId()),
        safe(reason));
  }

  public AuditRecord auditStanagCommandTranslated(AuditEvent auditEvent, AuditPayload... payloads)
      throws IOException {
    return auditLogger.audit(
        AuditMessages.STANAG_COMMAND_TRANSLATED,
        buildAuditContext(auditEvent, "stanag-command-translated", AuditOutcome.SUCCESS),
        writePayloads(auditEvent, payloads),
        safe(auditEvent.getStanagTaskType()),
        safe(auditEvent.getDroneCommandType()),
        safe(auditEvent.getDroneId()));
  }

  public AuditRecord auditDroneCommandDispatched(AuditEvent auditEvent, AuditPayload... payloads)
      throws IOException {
    return auditLogger.audit(
        AuditMessages.DRONE_COMMAND_DISPATCHED,
        buildAuditContext(auditEvent, "drone-command-dispatched", AuditOutcome.SUCCESS),
        writePayloads(auditEvent, payloads),
        safe(auditEvent.getDroneCommandType()),
        safe(auditEvent.getDroneId()),
        safe(auditEvent.getCommandId()));
  }

  public AuditRecord auditDroneCommandAcknowledged(AuditEvent auditEvent, AuditPayload... payloads)
      throws IOException {
    return auditLogger.audit(
        AuditMessages.DRONE_COMMAND_ACKNOWLEDGED,
        buildAuditContext(auditEvent, "drone-command-acknowledged", AuditOutcome.SUCCESS),
        writePayloads(auditEvent, payloads),
        safe(auditEvent.getDroneCommandType()),
        safe(auditEvent.getDroneId()),
        safe(auditEvent.getCommandId()));
  }

  public AuditRecord auditDroneCommandFailed(
      AuditEvent auditEvent, String reason, AuditPayload... payloads) throws IOException {
    return auditLogger.audit(
        AuditMessages.DRONE_COMMAND_FAILED,
        buildAuditContext(auditEvent, "drone-command-failed", AuditOutcome.FAILURE),
        writePayloads(auditEvent, payloads),
        safe(auditEvent.getDroneCommandType()),
        safe(auditEvent.getDroneId()),
        safe(reason));
  }

  public AuditRecord auditStanagTaskResultPublished(AuditEvent auditEvent, AuditPayload... payloads)
      throws IOException {
    return auditLogger.audit(
        AuditMessages.STANAG_TASK_RESULT_PUBLISHED,
        buildAuditContext(auditEvent, "stanag-task-result-published", AuditOutcome.SUCCESS),
        writePayloads(auditEvent, payloads),
        safe(auditEvent.getStanagTaskType()),
        safe(auditEvent.getDroneId()),
        safe(auditEvent.getTaskId()));
  }

  public AuditRecord auditStanagTaskResultFailed(
      AuditEvent auditEvent, String reason, AuditPayload... payloads) throws IOException {
    return auditLogger.audit(
        AuditMessages.STANAG_TASK_RESULT_FAILED,
        buildAuditContext(auditEvent, "stanag-task-result-failed", AuditOutcome.FAILURE),
        writePayloads(auditEvent, payloads),
        safe(auditEvent.getStanagTaskType()),
        safe(auditEvent.getDroneId()),
        safe(reason));
  }

  private AuditContext buildAuditContext(AuditEvent auditEvent, String action, AuditOutcome outcome) {
    AuditContext auditContext = AuditContext.builder()
        .auditId(auditEvent.getAuditId())
        .correlationId(auditEvent.getCorrelationId())
        .parentCorrelationId(auditEvent.getParentCorrelationId())
        .actor(auditEvent.getActor())
        .actorType(auditEvent.getActorType())
        .source(auditEvent.getSource())
        .destination(auditEvent.getDestination())
        .subject(auditEvent.getSubject())
        .action(action)
        .outcome(outcome)
        .timestamp(Instant.now())
        .build();

    addIfPresent(auditContext, "missionId", auditEvent.getMissionId());
    addIfPresent(auditContext, "taskId", auditEvent.getTaskId());
    addIfPresent(auditContext, "commandId", auditEvent.getCommandId());
    addIfPresent(auditContext, "droneId", auditEvent.getDroneId());
    addIfPresent(auditContext, "stanagTaskType", auditEvent.getStanagTaskType());
    addIfPresent(auditContext, "droneCommandType", auditEvent.getDroneCommandType());
    addIfPresent(auditContext, "protocol", auditEvent.getProtocol());
    addIfPresent(auditContext, "targetSystem", auditEvent.getTargetSystem());
    addIfPresent(auditContext, "targetComponent", auditEvent.getTargetComponent());

    for (Map.Entry<String, String> entry : auditEvent.getAttributes().entrySet()) {
      addIfPresent(auditContext, entry.getKey(), entry.getValue());
    }

    return auditContext;
  }

  private List<AuditPayloadReference> writePayloads(AuditEvent auditEvent, AuditPayload... payloads)
      throws IOException {
    List<AuditPayloadReference> payloadReferences = new ArrayList<>();

    if (payloads == null) {
      return payloadReferences;
    }

    for (AuditPayload payload : payloads) {
      if (payload == null || payload.getPayload() == null) {
        continue;
      }

      AuditPayloadReference payloadReference = auditPayloadStore.writePayload(
          safeIdentifier(auditEvent.getCorrelationId()),
          safe(payload.getName()),
          safeFileName(payload.getFileName()),
          payload.getPayload());

      payloadReference.setContentType(safe(payload.getContentType()));
      payloadReferences.add(payloadReference);
    }

    return payloadReferences;
  }

  private void addIfPresent(AuditContext auditContext, String name, String value) {
    if (value == null || value.isBlank()) {
      return;
    }

    auditContext.addAttribute(name, value);
  }

  private String safe(String value) {
    if (value == null) {
      return "";
    }

    return value;
  }

  private String safeIdentifier(String value) {
    if (value == null || value.isBlank()) {
      return "unknown-correlation";
    }

    return value;
  }

  private String safeFileName(String value) {
    if (value == null || value.isBlank()) {
      return "payload.bin";
    }

    return value.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}