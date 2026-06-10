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
import io.mapsmessaging.state.drone.model.GeoPosition;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
public class TaskAdminCommand {

  private static final String TASK_ADMIN_ACTION_PREFIX = "TaskAdminActionEnum_";
  private static final String TASK_TYPE_PREFIX = "TaskTypeEnum_";

  private final String action;
  private final String taskType;
  private final String identifier;
  private final String nodeIdentifier;
  private final UUID authorityGuid;
  private final String positionIdentifier;
  private final GeoPosition position;

  public TaskAdminCommand(
      String action,
      String taskType,
      String identifier,
      String nodeIdentifier,
      String authorityGuid,
      String positionIdentifier,
      GeoPosition position) {
    this.action = action;
    this.taskType = taskType;
    this.identifier = identifier;
    this.nodeIdentifier = nodeIdentifier;
    this.authorityGuid = UUID.fromString(authorityGuid);
    this.positionIdentifier = positionIdentifier;
    this.position = position;
  }

  public static TaskAdminCommand fromJson(JsonObject jsonObject) throws TaskAdminCommandException {
    try {
      JsonObject bodyObject = getRequiredObject(jsonObject, "body");

      String action = stripPrefix(
          getRequiredString(bodyObject, "action"),
          TASK_ADMIN_ACTION_PREFIX
      );

      String identifier = getRequiredString(bodyObject, "identifier");
      String nodeIdentifier = getRequiredString(bodyObject, "node");

      JsonObject authorityObject = getRequiredObject(bodyObject, "authority");
      validateDiscriminator(authorityObject, "AuthorityTypeEnum_GUID");
      String authorityGuid = getRequiredString(authorityObject, "guid");

      JsonObject descriptionObject = getRequiredObject(bodyObject, "description");

      String taskType = stripPrefix(
          getRequiredString(descriptionObject, "$discriminator"),
          TASK_TYPE_PREFIX
      );

      if (!"REPOSITION".equals(taskType)) {
        throw new TaskAdminCommandException("Unsupported task type: " + taskType);
      }

      JsonObject repositionObject = getRequiredObject(descriptionObject, "reposition");
      JsonObject locationObject = getRequiredObject(repositionObject, "location");

      String positionIdentifier = getRequiredString(locationObject, "identifier");

      JsonObject geometryObject = getRequiredObject(locationObject, "location");
      validateDiscriminator(geometryObject, "GeometryTypeEnum_POINT");

      JsonObject pointObject = getRequiredObject(geometryObject, "point");

      GeoPosition position = new GeoPosition();
      position.setLatitude(getRequiredDouble(pointObject, "latitude"));
      position.setLongitude(getRequiredDouble(pointObject, "longitude"));
      position.setAltitudeMslMeters(getRequiredDouble(pointObject, "altitude"));

      return new TaskAdminCommand(
          action,
          taskType,
          identifier,
          nodeIdentifier,
          authorityGuid,
          positionIdentifier,
          position
      );
    } catch (IllegalStateException | ClassCastException exception) {
      throw new TaskAdminCommandException("Invalid TASK_ADMIN command structure", exception);
    }
  }

  private static String stripPrefix(String value, String prefix) {
    if (value != null && value.startsWith(prefix)) {
      return value.substring(prefix.length());
    }
    return value;
  }

  private static void validateDiscriminator(JsonObject jsonObject, String expectedValue)
      throws TaskAdminCommandException {
    String actualValue = getRequiredString(jsonObject, "$discriminator");
    if (!expectedValue.equals(actualValue)) {
      throw new TaskAdminCommandException(
          "Unsupported discriminator: " + actualValue + ", expected: " + expectedValue);
    }
  }

  private static JsonObject getRequiredObject(JsonObject jsonObject, String name)
      throws TaskAdminCommandException {
    if (jsonObject == null || !jsonObject.has(name) || jsonObject.get(name).isJsonNull()) {
      throw new TaskAdminCommandException("Missing required object: " + name);
    }
    if (!jsonObject.get(name).isJsonObject()) {
      throw new TaskAdminCommandException("Required field is not an object: " + name);
    }
    return jsonObject.getAsJsonObject(name);
  }

  private static String getRequiredString(JsonObject jsonObject, String name)
      throws TaskAdminCommandException {
    if (jsonObject == null || !jsonObject.has(name) || jsonObject.get(name).isJsonNull()) {
      throw new TaskAdminCommandException("Missing required field: " + name);
    }
    return jsonObject.get(name).getAsString();
  }

  private static Double getRequiredDouble(JsonObject jsonObject, String name)
      throws TaskAdminCommandException {
    if (jsonObject == null || !jsonObject.has(name) || jsonObject.get(name).isJsonNull()) {
      throw new TaskAdminCommandException("Missing required field: " + name);
    }
    return jsonObject.get(name).getAsDouble();
  }
}