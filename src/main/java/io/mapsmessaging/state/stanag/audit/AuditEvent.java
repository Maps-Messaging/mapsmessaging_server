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

import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

  private String auditId;
  private String correlationId;
  private String parentCorrelationId;

  private String actor;
  private String actorType;
  private String source;
  private String destination;
  private String subject;

  private String missionId;
  private String taskId;
  private String commandId;
  private String droneId;

  private String stanagTaskType;
  private String droneCommandType;
  private String protocol;

  private String targetSystem;
  private String targetComponent;

  private Map<String, String> attributes;

  public Map<String, String> getAttributes() {
    if (attributes == null) {
      attributes = new LinkedHashMap<>();
    }

    return attributes;
  }

  public void addAttribute(String name, String value) {
    getAttributes().put(name, value);
  }
}