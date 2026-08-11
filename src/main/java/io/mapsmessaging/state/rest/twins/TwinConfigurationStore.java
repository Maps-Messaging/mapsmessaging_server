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

package io.mapsmessaging.state.rest.twins;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.TwinPublishConfigDTO;
import io.mapsmessaging.state.config.capability.Authorities;
import io.mapsmessaging.state.config.capability.PlanTaskType;
import io.mapsmessaging.state.config.capability.TaskCapabilities;
import io.mapsmessaging.state.config.capability.TaskCapability;
import io.mapsmessaging.state.config.geospatial.GeoSpatialAreaConfigDTO;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.mavlink.model.ModelManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

class TwinConfigurationStore {

  private static final Pattern DRONE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

  private final TwinManagerConfig config;

  TwinConfigurationStore(TwinManagerConfig config) {
    this.config = config;
  }

  TwinManagerConfig getConfig() {
    return config;
  }

  TwinCoreConfigDTO getCoreConfig() {
    TwinCoreConfigDTO coreConfig = new TwinCoreConfigDTO();
    coreConfig.setHeartbeatTimeoutMillis(config.getHeartbeatTimeoutMillis());
    coreConfig.setStaleTimeoutMillis(config.getStaleTimeoutMillis());
    coreConfig.setRetentionTimeoutMillis(config.getRetentionTimeoutMillis());
    coreConfig.setRemoveExpiredTwins(config.isRemoveExpiredTwins());
    coreConfig.setDefaultRootPath(config.getDefaultRootPath());
    return coreConfig;
  }

  void updateCoreConfig(TwinCoreConfigDTO coreConfig) throws IOException {
    validateCoreConfig(coreConfig);
    config.setHeartbeatTimeoutMillis(coreConfig.getHeartbeatTimeoutMillis());
    config.setStaleTimeoutMillis(coreConfig.getStaleTimeoutMillis());
    config.setRetentionTimeoutMillis(coreConfig.getRetentionTimeoutMillis());
    config.setRemoveExpiredTwins(coreConfig.isRemoveExpiredTwins());
    config.setDefaultRootPath(coreConfig.getDefaultRootPath());
    config.save();
  }

  Optional<TakProtocolDTO> getTakConfig() {
    return Optional.ofNullable(config.getTak());
  }

  void putTakConfig(TakProtocolDTO takConfig) throws IOException {
    validateTakConfig(takConfig);
    config.setTak(takConfig);
    config.save();
  }

  void deleteTakConfig() throws IOException {
    if (config.getTak() == null) {
      throw new TwinConfigurationException("TAK configuration is not configured", 404);
    }
    config.setTak(null);
    config.save();
  }

  Optional<TwinPublishConfigDTO> getPublishConfig() {
    return Optional.ofNullable(config.getPublish());
  }

  void putPublishConfig(TwinPublishConfigDTO publishConfig) throws IOException {
    validatePublishConfig(publishConfig);
    config.setPublish(publishConfig);
    config.save();
  }

  void deletePublishConfig() throws IOException {
    if (config.getPublish() == null) {
      throw new TwinConfigurationException("Publish configuration is not configured", 404);
    }
    config.setPublish(null);
    config.save();
  }

  Optional<N2KTwinConfig> getN2kConfig() {
    return Optional.ofNullable(config.getN2KTwinConfig());
  }

  void putN2kConfig(N2KTwinConfig n2kConfig) throws IOException {
    validateN2kConfig(n2kConfig);
    config.setN2KTwinConfig(n2kConfig);
    config.save();
  }

  void deleteN2kConfig() throws IOException {
    if (config.getN2KTwinConfig() == null) {
      throw new TwinConfigurationException("N2K configuration is not configured", 404);
    }
    N2KTwinConfig disabledConfig = new N2KTwinConfig();
    disabledConfig.setEnable(false);
    disabledConfig.setPublishMavlinkDrones(false);
    config.setN2KTwinConfig(disabledConfig);
    config.save();
  }

  List<MavlinkTwinConfigDTO> listMavlinkSources() {
    return config.getMavlink();
  }

  Optional<MavlinkTwinConfigDTO> getMavlinkSource(String name) {
    return config.getMavlink().stream()
        .filter(entry -> name.equals(entry.getName()))
        .findFirst();
  }

  void createMavlinkSource(MavlinkTwinConfigDTO mavlinkConfig) throws IOException {
    validateMavlinkSource(mavlinkConfig);
    if (getMavlinkSource(mavlinkConfig.getName()).isPresent()) {
      throw new TwinConfigurationException("MAVLink twin source already exists: " + mavlinkConfig.getName(), 409);
    }
    config.getMavlink().add(mavlinkConfig);
    config.save();
  }

  void updateMavlinkSource(String name, MavlinkTwinConfigDTO mavlinkConfig) throws IOException {
    validateName(name, "name");
    validateMavlinkSource(mavlinkConfig);
    if (!name.equals(mavlinkConfig.getName())) {
      throw new TwinConfigurationException("MAVLink twin source name cannot be changed", 400);
    }

    List<MavlinkTwinConfigDTO> mavlinkSources = config.getMavlink();
    for (int index = 0; index < mavlinkSources.size(); index++) {
      if (name.equals(mavlinkSources.get(index).getName())) {
        mavlinkSources.set(index, mavlinkConfig);
        config.save();
        return;
      }
    }
    throw new TwinConfigurationException("Unknown MAVLink twin source: " + name, 404);
  }

  void deleteMavlinkSource(String name) throws IOException {
    validateName(name, "name");
    if (!config.getMavlink().removeIf(entry -> name.equals(entry.getName()))) {
      throw new TwinConfigurationException("Unknown MAVLink twin source: " + name, 404);
    }
    config.save();
  }

  List<DroneInfoDTO> listDrones() {
    return config.getDroneInfo();
  }

  List<String> listDroneModelNames() {
    return ModelManager.getInstance().getModelNames().stream().sorted().toList();
  }

  List<String> listGeospatialAreaNames() {
    if (config.getGeospatial() == null || config.getGeospatial().getAreas() == null) {
      return List.of();
    }
    return config.getGeospatial().getAreas().stream().map(GeoSpatialAreaConfigDTO::getName).filter(name -> name != null && !name.isBlank()).map(String::trim).distinct().sorted().toList();
  }

  Optional<DroneInfoDTO> getDrone(String name) {
    return config.getDroneInfo().stream()
        .filter(entry -> name.equals(entry.getName()))
        .findFirst();
  }

  void createDrone(DroneInfoDTO droneInfo) throws IOException {
    validateDroneConfiguration(droneInfo);

    synchronized (config) {
      List<DroneInfoDTO> droneInfos = config.getDroneInfo();
      if (droneInfos.stream().anyMatch(entry -> droneInfo.getName().equalsIgnoreCase(entry.getName()))) {
        throw new TwinConfigurationException("Drone configuration already exists: " + droneInfo.getName(), 409);
      }
      if (droneInfos.stream().anyMatch(entry -> droneInfo.getUuid().equals(entry.getUuid()))) {
        throw new TwinConfigurationException("Drone UUID is already configured: " + droneInfo.getUuid(), 409);
      }

      droneInfos.add(droneInfo);
      try {
        config.save();
      } catch (IOException | RuntimeException exception) {
        droneInfos.remove(droneInfo);
        throw exception;
      }
    }
  }

  void updateDrone(String name, DroneInfoDTO droneInfo) throws IOException {
    String droneName = validateDroneName(name);
    validateDroneConfiguration(droneInfo);
    if (!droneName.equals(droneInfo.getName())) {
      throw new TwinConfigurationException("Drone configuration name cannot be changed", 400);
    }

    synchronized (config) {
      List<DroneInfoDTO> droneInfos = config.getDroneInfo();
      int droneIndex = -1;
      for (int index = 0; index < droneInfos.size(); index++) {
        if (droneName.equals(droneInfos.get(index).getName())) {
          droneIndex = index;
          break;
        }
      }
      if (droneIndex < 0) {
        throw new TwinConfigurationException("Unknown drone configuration: " + droneName, 404);
      }

      DroneInfoDTO existingDrone = droneInfos.get(droneIndex);
      if (!Objects.equals(existingDrone.getUuid(), droneInfo.getUuid())) {
        throw new TwinConfigurationException("Drone UUID cannot be changed: " + droneName, 409);
      }
      for (int index = 0; index < droneInfos.size(); index++) {
        if (index == droneIndex) {
          continue;
        }
        DroneInfoDTO otherDrone = droneInfos.get(index);
        if (droneInfo.getName().equalsIgnoreCase(otherDrone.getName())) {
          throw new TwinConfigurationException("Drone configuration name is already in use: " + droneInfo.getName(), 409);
        }
        if (droneInfo.getUuid().equals(otherDrone.getUuid())) {
          throw new TwinConfigurationException("Drone UUID is already configured: " + droneInfo.getUuid(), 409);
        }
      }

      droneInfos.set(droneIndex, droneInfo);
      try {
        config.save();
      } catch (IOException | RuntimeException exception) {
        droneInfos.set(droneIndex, existingDrone);
        throw exception;
      }
    }
  }

  void deleteDrone(String name) throws IOException {
    String droneName = validateDroneName(name);

    synchronized (config) {
      List<DroneInfoDTO> droneInfos = config.getDroneInfo();
      int droneIndex = -1;
      for (int index = 0; index < droneInfos.size(); index++) {
        if (droneName.equals(droneInfos.get(index).getName())) {
          droneIndex = index;
          break;
        }
      }
      if (droneIndex < 0) {
        throw new TwinConfigurationException("Unknown drone configuration: " + droneName, 404);
      }

      DroneInfoDTO removedDrone = droneInfos.remove(droneIndex);
      List<MavlinkTwinConfigDTO> mavlinkSources = config.getMavlink();
      List<List<MavlinkKnownSourceDTO>> originalKnownSources = new ArrayList<>(mavlinkSources.size());
      for (MavlinkTwinConfigDTO mavlinkSource : mavlinkSources) {
        List<MavlinkKnownSourceDTO> knownSources = mavlinkSource.getKnownSources();
        originalKnownSources.add(knownSources);
        if (knownSources != null && knownSources.stream().anyMatch(knownSource -> knownSource != null && droneName.equals(knownSource.getName()))) {
          List<MavlinkKnownSourceDTO> remainingSources = new ArrayList<>(knownSources);
          remainingSources.removeIf(knownSource -> knownSource != null && droneName.equals(knownSource.getName()));
          mavlinkSource.setKnownSources(remainingSources);
        }
      }

      try {
        config.save();
      } catch (IOException | RuntimeException exception) {
        droneInfos.add(droneIndex, removedDrone);
        for (int index = 0; index < mavlinkSources.size(); index++) {
          mavlinkSources.get(index).setKnownSources(originalKnownSources.get(index));
        }
        throw exception;
      }
    }
  }

  void updateAuthorityBindings(String uuid, AuthorityBindingsUpdateDTO update) throws IOException {
    UUID authorityId = validateAuthorityUuid(uuid);
    if (update == null) {
      throw new TwinConfigurationException("Authority binding update is required", 400);
    }

    Set<String> addDrones = validateDroneNames(update.getAddDrones(), "addDrones");
    Set<String> removeDrones = validateDroneNames(update.getRemoveDrones(), "removeDrones");
    if (addDrones.isEmpty() && removeDrones.isEmpty()) {
      throw new TwinConfigurationException("At least one drone binding change is required", 400);
    }
    Set<String> overlappingDrones = new HashSet<>(addDrones);
    overlappingDrones.retainAll(removeDrones);
    if (!overlappingDrones.isEmpty()) {
      throw new TwinConfigurationException("A drone cannot be added and removed in the same authority update: " + overlappingDrones.iterator().next(), 400);
    }

    synchronized (config) {
      Map<String, DroneInfoDTO> dronesByName = new LinkedHashMap<>();
      for (DroneInfoDTO droneInfo : config.getDroneInfo()) {
        dronesByName.put(droneInfo.getName(), droneInfo);
      }
      Set<String> requestedDrones = new HashSet<>(addDrones);
      requestedDrones.addAll(removeDrones);
      for (String droneName : requestedDrones) {
        if (!dronesByName.containsKey(droneName)) {
          throw new TwinConfigurationException("Unknown drone configuration: " + droneName, 404);
        }
      }
      for (String droneName : addDrones) {
        DroneInfoDTO droneInfo = dronesByName.get(droneName);
        if (droneInfo.getCapabilities() == null || droneInfo.getCapabilities().getTasks() == null || droneInfo.getCapabilities().getTasks().isEmpty()) {
          throw new TwinConfigurationException("Drone has no task capabilities to bind: " + droneName, 400);
        }
      }

      Map<TaskCapability, Authorities[]> originalAuthorities = new IdentityHashMap<>();
      boolean changed = false;
      for (String droneName : addDrones) {
        changed |= setAuthorityBinding(dronesByName.get(droneName), authorityId, true, originalAuthorities);
      }
      for (String droneName : removeDrones) {
        changed |= setAuthorityBinding(dronesByName.get(droneName), authorityId, false, originalAuthorities);
      }
      if (!changed) {
        return;
      }

      try {
        config.save();
      } catch (IOException | RuntimeException exception) {
        originalAuthorities.forEach(TaskCapability::setAuthorities);
        throw exception;
      }
    }
  }

  void deleteAuthority(String uuid) throws IOException {
    UUID authorityId = validateAuthorityUuid(uuid);

    synchronized (config) {
      Map<TaskCapability, Authorities[]> originalAuthorities = new IdentityHashMap<>();
      boolean changed = false;
      for (DroneInfoDTO droneInfo : config.getDroneInfo()) {
        changed |= setAuthorityBinding(droneInfo, authorityId, false, originalAuthorities);
      }
      if (!changed) {
        throw new TwinConfigurationException("Unknown authority UUID: " + authorityId, 404);
      }

      try {
        config.save();
      } catch (IOException | RuntimeException exception) {
        originalAuthorities.forEach(TaskCapability::setAuthorities);
        throw exception;
      }
    }
  }

  Map<String, ConfigurationProperties> listAdapterConfigs() {
    return config.getAdapterConfig();
  }

  Optional<ConfigurationProperties> getAdapterConfig(String name) {
    return Optional.ofNullable(config.getAdapterConfig().get(name));
  }

  void createAdapterConfig(String name, ConfigurationProperties adapterConfig) throws IOException {
    validateName(name, "name");
    validateAdapterConfig(adapterConfig);
    if (config.getAdapterConfig().containsKey(name)) {
      throw new TwinConfigurationException("State adapter configuration already exists: " + name, 409);
    }
    config.getAdapterConfig().put(name, adapterConfig);
    config.save();
  }

  void updateAdapterConfig(String name, ConfigurationProperties adapterConfig) throws IOException {
    validateName(name, "name");
    validateAdapterConfig(adapterConfig);
    if (!config.getAdapterConfig().containsKey(name)) {
      throw new TwinConfigurationException("Unknown state adapter configuration: " + name, 404);
    }
    config.getAdapterConfig().put(name, adapterConfig);
    config.save();
  }

  void deleteAdapterConfig(String name) throws IOException {
    validateName(name, "name");
    if (config.getAdapterConfig().remove(name) == null) {
      throw new TwinConfigurationException("Unknown state adapter configuration: " + name, 404);
    }
    config.save();
  }

  void replaceAdapterConfigs(Map<String, ConfigurationProperties> adapterConfigs) throws IOException {
    if (adapterConfigs == null) {
      throw new TwinConfigurationException("State adapter configuration map is required", 400);
    }
    config.setAdapterConfig(new LinkedHashMap<>(adapterConfigs));
    config.save();
  }

  private void validateCoreConfig(TwinCoreConfigDTO coreConfig) {
    if (coreConfig == null) {
      throw new TwinConfigurationException("Core twin configuration is required", 400);
    }
    if (coreConfig.getHeartbeatTimeoutMillis() <= 0) {
      throw new TwinConfigurationException("heartbeatTimeoutMillis must be greater than 0", 400);
    }
    if (coreConfig.getStaleTimeoutMillis() <= 0) {
      throw new TwinConfigurationException("staleTimeoutMillis must be greater than 0", 400);
    }
    if (coreConfig.getRetentionTimeoutMillis() < 0) {
      throw new TwinConfigurationException("retentionTimeoutMillis must be greater than or equal to 0", 400);
    }
    if (coreConfig.getDefaultRootPath() == null || coreConfig.getDefaultRootPath().isBlank()) {
      throw new TwinConfigurationException("defaultRootPath is required", 400);
    }
  }

  private void validateTakConfig(TakProtocolDTO takConfig) {
    if (takConfig == null) {
      throw new TwinConfigurationException("TAK configuration is required", 400);
    }
    if (takConfig.getHostname() == null || takConfig.getHostname().isBlank()) {
      throw new TwinConfigurationException("hostname is required", 400);
    }
    if (takConfig.getPort() <= 0 || takConfig.getPort() > 65535) {
      throw new TwinConfigurationException("port must be between 1 and 65535", 400);
    }
  }

  private void validatePublishConfig(TwinPublishConfigDTO publishConfig) {
    if (publishConfig == null) {
      throw new TwinConfigurationException("Publish configuration is required", 400);
    }
    if (publishConfig.isEnabled() && (publishConfig.getTopicTemplate() == null || publishConfig.getTopicTemplate().isBlank())) {
      throw new TwinConfigurationException("topicTemplate is required when publishing is enabled", 400);
    }
  }

  private void validateN2kConfig(N2KTwinConfig n2kConfig) {
    if (n2kConfig == null) {
      throw new TwinConfigurationException("N2K configuration is required", 400);
    }
    if (n2kConfig.isEnable() && (n2kConfig.getTopic() == null || n2kConfig.getTopic().isBlank())) {
      throw new TwinConfigurationException("topic is required when N2K is enabled", 400);
    }
  }

  private void validateAdapterConfig(ConfigurationProperties adapterConfig) {
    if (adapterConfig == null) {
      throw new TwinConfigurationException("State adapter configuration is required", 400);
    }
  }

  private void validateMavlinkSource(MavlinkTwinConfigDTO mavlinkConfig) {
    if (mavlinkConfig == null) {
      throw new TwinConfigurationException("MAVLink twin source configuration is required", 400);
    }
    validateName(mavlinkConfig.getName(), "name");
    if (mavlinkConfig.getTopic() == null || mavlinkConfig.getTopic().isBlank()) {
      throw new TwinConfigurationException("topic is required", 400);
    }
  }

  private void validateDrone(DroneInfoDTO droneInfo) {
    if (droneInfo == null) {
      throw new TwinConfigurationException("Drone configuration is required", 400);
    }
    validateName(droneInfo.getName(), "name");
    if (droneInfo.getUuid() == null) {
      throw new TwinConfigurationException("uuid is required", 400);
    }
  }

  private void validateDroneConfiguration(DroneInfoDTO droneInfo) {
    validateDrone(droneInfo);

    droneInfo.setName(validateDroneName(droneInfo.getName()));

    if (droneInfo.getModelName() == null || droneInfo.getModelName().isBlank()) {
      throw new TwinConfigurationException("modelName is required", 400);
    }
    droneInfo.setModelName(droneInfo.getModelName().trim());
    if (ModelManager.getInstance().getModel(droneInfo.getModelName()).isEmpty()) {
      throw new TwinConfigurationException("Unknown drone model: " + droneInfo.getModelName(), 400);
    }

    if (droneInfo.getGeospatialArea() != null) {
      String geospatialArea = droneInfo.getGeospatialArea().trim();
      droneInfo.setGeospatialArea(geospatialArea.isEmpty() ? null : geospatialArea);
      if (!geospatialArea.isEmpty() && !listGeospatialAreaNames().contains(geospatialArea)) {
        throw new TwinConfigurationException("Unknown geospatial area: " + geospatialArea, 400);
      }
    }

    if (droneInfo.getMessageEncoding() == null) {
      throw new TwinConfigurationException("messageEncoding is required", 400);
    }
    if (droneInfo.getCancelAction() == null || droneInfo.getMissionEndAction() == null || droneInfo.getMissionTimeoutAction() == null) {
      throw new TwinConfigurationException("cancelAction, missionEndAction and missionTimeoutAction are required", 400);
    }

    validateNonNegativeFinite(droneInfo.getBatteryCapacityAh(), "batteryCapacityAh");
    validateNonNegativeFinite(droneInfo.getBatteryCapacityHours(), "batteryCapacityHours");
    validateOptionalPositiveFinite(droneInfo.getRangeMeters(), "rangeMeters");
    validateOptionalPositiveFinite(droneInfo.getSurveyRadiusMeters(), "surveyRadiusMeters");
    validateTaskCapabilities(droneInfo.getCapabilities());
  }

  private void validateTaskCapabilities(TaskCapabilities capabilities) {
    if (capabilities == null || capabilities.getTasks() == null) {
      throw new TwinConfigurationException("capabilities and task_capabilities are required", 400);
    }
    if (capabilities.getTaskConditionsMode() == null || capabilities.getTaskConditionsTemplate() == null) {
      throw new TwinConfigurationException("task condition modes are required", 400);
    }

    Set<PlanTaskType> taskTypes = new HashSet<>();
    for (TaskCapability capability : capabilities.getTasks()) {
      if (capability == null || capability.getTaskType() == null) {
        throw new TwinConfigurationException("Every task capability requires a task type", 400);
      }
      if (!taskTypes.add(capability.getTaskType())) {
        throw new TwinConfigurationException("Task type is configured more than once: " + capability.getTaskType(), 400);
      }
      if (capability.getSpecialization() == null) {
        throw new TwinConfigurationException("Every task capability requires a specialization", 400);
      }
      validateAuthorities(capability.getAuthorities(), capability.getTaskType());
    }
  }

  private void validateAuthorities(Authorities[] authorities, PlanTaskType taskType) {
    if (authorities == null) {
      return;
    }

    Set<UUID> authorityIds = new HashSet<>();
    for (Authorities authority : authorities) {
      if (authority == null || authority.getGuid() == null) {
        throw new TwinConfigurationException("Every authority for task type " + taskType + " requires a UUID", 400);
      }
      if (!authorityIds.add(authority.getGuid())) {
        throw new TwinConfigurationException("Authority UUID is configured more than once for task type " + taskType + ": " + authority.getGuid(), 400);
      }
    }
  }

  private void validateNonNegativeFinite(double value, String fieldName) {
    if (!Double.isFinite(value) || value < 0.0d) {
      throw new TwinConfigurationException(fieldName + " must be a finite value greater than or equal to zero", 400);
    }
  }

  private void validateOptionalPositiveFinite(Double value, String fieldName) {
    if (value != null && (!Double.isFinite(value) || value <= 0.0d)) {
      throw new TwinConfigurationException(fieldName + " must be a finite value greater than zero when supplied", 400);
    }
  }

  private void validateName(String name, String fieldName) {
    if (name == null || name.isBlank()) {
      throw new TwinConfigurationException(fieldName + " is required", 400);
    }
  }

  private String validateDroneName(String name) {
    validateName(name, "name");
    String droneName = name.trim();
    if (!DRONE_NAME_PATTERN.matcher(droneName).matches()) {
      throw new TwinConfigurationException("name must contain only letters, numbers, '.', '_' or '-' and must not exceed 128 characters", 400);
    }
    return droneName;
  }

  private UUID validateAuthorityUuid(String uuid) {
    if (uuid == null || uuid.isBlank()) {
      throw new TwinConfigurationException("Authority UUID is required", 400);
    }
    try {
      String value = uuid.trim();
      UUID authorityId = UUID.fromString(value);
      if (!authorityId.toString().equalsIgnoreCase(value)) {
        throw new IllegalArgumentException("Non-canonical UUID");
      }
      return authorityId;
    } catch (IllegalArgumentException exception) {
      throw new TwinConfigurationException("Invalid authority UUID: " + uuid, 400);
    }
  }

  private Set<String> validateDroneNames(List<String> droneNames, String fieldName) {
    if (droneNames == null) {
      throw new TwinConfigurationException(fieldName + " is required", 400);
    }
    Set<String> names = new HashSet<>();
    for (String droneName : droneNames) {
      String validatedName = validateDroneName(droneName);
      if (!names.add(validatedName)) {
        throw new TwinConfigurationException(fieldName + " contains the same drone more than once: " + validatedName, 400);
      }
    }
    return names;
  }

  private boolean setAuthorityBinding(DroneInfoDTO droneInfo, UUID authorityId, boolean enabled, Map<TaskCapability, Authorities[]> originalAuthorities) {
    if (droneInfo.getCapabilities() == null || droneInfo.getCapabilities().getTasks() == null) {
      return false;
    }

    boolean changed = false;
    for (TaskCapability task : droneInfo.getCapabilities().getTasks()) {
      if (task == null) {
        continue;
      }
      Authorities[] authorities = task.getAuthorities();
      List<Authorities> updatedAuthorities = new ArrayList<>();
      int matchingAuthorities = 0;
      if (authorities != null) {
        for (Authorities authority : authorities) {
          if (authority != null && authorityId.equals(authority.getGuid())) {
            matchingAuthorities++;
          } else {
            updatedAuthorities.add(authority);
          }
        }
      }
      if (enabled) {
        updatedAuthorities.add(new Authorities(authorityId));
      }
      if ((!enabled && matchingAuthorities == 0) || (enabled && matchingAuthorities == 1)) {
        continue;
      }
      originalAuthorities.put(task, authorities);
      task.setAuthorities(updatedAuthorities.toArray(new Authorities[0]));
      changed = true;
    }
    return changed;
  }

  static class TwinConfigurationException extends RuntimeException {

    private final int statusCode;

    TwinConfigurationException(String message, int statusCode) {
      super(message);
      this.statusCode = statusCode;
    }

    int getStatusCode() {
      return statusCode;
    }
  }
}
