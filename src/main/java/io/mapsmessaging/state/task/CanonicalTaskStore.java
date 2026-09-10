/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.task;

import io.mapsmessaging.state.drone.model.GeoPosition;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class CanonicalTaskStore {

  private final Path root;

  public CanonicalTaskStore(Path root) {
    this.root = root;
  }

  public synchronized void save(CanonicalTask task) throws IOException {
    Files.createDirectories(root);
    Properties properties = encode(task);
    Path target = root.resolve(task.getCanonicalTaskId() + ".properties");
    Path temporary = root.resolve(task.getCanonicalTaskId() + ".tmp");
    try (OutputStream output = Files.newOutputStream(temporary)) {
      properties.store(output, "MapsMessaging canonical task v1");
    }
    try {
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public synchronized List<CanonicalTask> load() throws IOException {
    if (!Files.isDirectory(root)) {
      return List.of();
    }
    List<CanonicalTask> tasks = new ArrayList<>();
    try (DirectoryStream<Path> files = Files.newDirectoryStream(root, "*.properties")) {
      for (Path file : files) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
          properties.load(input);
        }
        tasks.add(decode(properties));
      }
    }
    return tasks;
  }

  private Properties encode(CanonicalTask task) {
    Properties properties = new Properties();
    put(properties, "canonicalTaskId", task.getCanonicalTaskId());
    put(properties, "originProtocol", task.getOriginProtocol());
    put(properties, "requester", task.getRequester());
    put(properties, "twinId", task.getTwinId());
    put(properties, "twinUuid", task.getTwinUuid());
    put(properties, "taskType", task.getTaskType());
    put(properties, "action", task.getAction());
    put(properties, "targetEntityId", task.getTargetEntityId());
    put(properties, "startTime", task.getStartTime());
    put(properties, "endTime", task.getEndTime());
    put(properties, "speedMetersPerSecond", task.getSpeedMetersPerSecond());
    put(properties, "arrivalToleranceMeters", task.getArrivalToleranceMeters());
    put(properties, "state", task.getState());
    put(properties, "responseDestination", task.getResponseDestination());
    put(properties, "resultReason", task.getResultReason());
    put(properties, "lastUpdateProtocol", task.getLastUpdateProtocol());
    put(properties, "originalPayloadBase64", task.getOriginalPayloadBase64());
    GeoPosition target = task.getTargetPosition();
    if (target != null) {
      put(properties, "target.latitude", target.getLatitude());
      put(properties, "target.longitude", target.getLongitude());
      put(properties, "target.altitudeMslMeters", target.getAltitudeMslMeters());
      put(properties, "target.altitudeAglMeters", target.getAltitudeAglMeters());
    }
    task.getProtocolTaskIds().forEach((key, value) -> put(properties, "protocolId." + key, value));
    for (int index = 0; index < task.getTranslationWarnings().size(); index++) {
      put(properties, "warning." + index, task.getTranslationWarnings().get(index));
    }
    return properties;
  }

  private CanonicalTask decode(Properties properties) {
    CanonicalTask task = new CanonicalTask();
    task.setCanonicalTaskId(UUID.fromString(required(properties, "canonicalTaskId")));
    task.setOriginProtocol(properties.getProperty("originProtocol"));
    task.setRequester(properties.getProperty("requester"));
    task.setTwinId(properties.getProperty("twinId"));
    task.setTwinUuid(uuid(properties.getProperty("twinUuid")));
    task.setTaskType(enumValue(CanonicalTaskType.class, properties.getProperty("taskType")));
    task.setAction(enumValue(CanonicalTaskAction.class, properties.getProperty("action")));
    task.setTargetEntityId(properties.getProperty("targetEntityId"));
    task.setStartTime(instant(properties.getProperty("startTime")));
    task.setEndTime(instant(properties.getProperty("endTime")));
    task.setSpeedMetersPerSecond(number(properties.getProperty("speedMetersPerSecond")));
    task.setArrivalToleranceMeters(number(properties.getProperty("arrivalToleranceMeters")));
    task.setState(enumValue(CanonicalTaskState.class, properties.getProperty("state")));
    task.setResponseDestination(properties.getProperty("responseDestination"));
    task.setResultReason(properties.getProperty("resultReason"));
    task.setLastUpdateProtocol(properties.getProperty("lastUpdateProtocol"));
    task.setOriginalPayloadBase64(properties.getProperty("originalPayloadBase64"));
    if (properties.containsKey("target.latitude") || properties.containsKey("target.longitude")) {
      task.setTargetPosition(new GeoPosition(
          number(properties.getProperty("target.latitude")),
          number(properties.getProperty("target.longitude")),
          number(properties.getProperty("target.altitudeMslMeters")),
          number(properties.getProperty("target.altitudeAglMeters"))));
    }
    Map<String, String> protocolIds = new LinkedHashMap<>();
    List<String> warnings = new ArrayList<>();
    properties.stringPropertyNames().stream().sorted().forEach(name -> {
      if (name.startsWith("protocolId.")) {
        protocolIds.put(name.substring("protocolId.".length()), properties.getProperty(name));
      } else if (name.startsWith("warning.")) {
        warnings.add(properties.getProperty(name));
      }
    });
    task.setProtocolTaskIds(protocolIds);
    task.setTranslationWarnings(warnings);
    return task;
  }

  private void put(Properties properties, String key, Object value) {
    if (value != null) {
      properties.setProperty(key, String.valueOf(value));
    }
  }

  private String required(Properties properties, String key) {
    String value = properties.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing canonical task property " + key);
    }
    return value;
  }

  private Instant instant(String value) {
    return value == null ? null : Instant.parse(value);
  }

  private UUID uuid(String value) {
    return value == null ? null : UUID.fromString(value);
  }

  private Double number(String value) {
    return value == null ? null : Double.valueOf(value);
  }

  private <T extends Enum<T>> T enumValue(Class<T> type, String value) {
    return value == null ? null : Enum.valueOf(type, value);
  }
}
