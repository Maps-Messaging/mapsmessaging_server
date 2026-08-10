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

package io.mapsmessaging.state.config.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.mapsmessaging.configuration.SystemProperties;
import org.junit.jupiter.api.Test;

class PlanTaskTypeTest {

  private final Gson gson = SystemProperties.getInstance().getGson();

  @Test
  void parsesLegacyTaskTypeValue() {
    TaskCapability capability =
        gson.fromJson("{\"task_type\":\"NAVIGATE\"}", TaskCapability.class);

    assertEquals(PlanTaskType.NAVIGATE, capability.getTaskType());
  }

  @Test
  void stripsPlanTaskTypePrefix() {
    TaskCapability capability =
        gson.fromJson(
            "{\"task_type\":\"PlanTaskType_SCREEN\"}",
            TaskCapability.class);

    assertEquals(PlanTaskType.SCREEN, capability.getTaskType());
  }

  @Test
  void stripsPlanTaskTypeEnumPrefix() {
    TaskCapability capability =
        gson.fromJson(
            "{\"task_type\":\"PlanTaskTypeEnum_SCREEN\"}",
            TaskCapability.class);

    assertEquals(PlanTaskType.SCREEN, capability.getTaskType());
  }

  @Test
  void serializesUsingCanonicalPlanTaskTypePrefix() {
    TaskCapability capability = new TaskCapability();
    capability.setTaskType(PlanTaskType.SCREEN);

    assertEquals("PlanTaskType_SCREEN", serializedTaskType(capability));
  }

  @Test
  void allAcceptedInputsSerializeToCanonicalWireValue() {
    assertCanonicalRoundTrip("SCREEN");
    assertCanonicalRoundTrip("PlanTaskType_SCREEN");
    assertCanonicalRoundTrip("PlanTaskTypeEnum_SCREEN");
  }

  @Test
  void rejectsUnknownPrefixedTaskType() {
    assertThrows(
        JsonParseException.class,
        () ->
            gson.fromJson(
                "{\"task_type\":\"PlanTaskType_NOT_A_TASK\"}",
                TaskCapability.class));
  }

  @Test
  void rejectsUnknownEnumPrefixedTaskType() {
    assertThrows(
        JsonParseException.class,
        () ->
            gson.fromJson(
                "{\"task_type\":\"PlanTaskTypeEnum_NOT_A_TASK\"}",
                TaskCapability.class));
  }

  private void assertCanonicalRoundTrip(String taskType) {
    TaskCapability capability =
        gson.fromJson(
            "{\"task_type\":\"" + taskType + "\"}",
            TaskCapability.class);

    assertEquals("PlanTaskType_SCREEN", serializedTaskType(capability));
  }

  private String serializedTaskType(TaskCapability capability) {
    JsonObject serialized =
        JsonParser.parseString(gson.toJson(capability)).getAsJsonObject();
    return serialized.get("task_type").getAsString();
  }
}
