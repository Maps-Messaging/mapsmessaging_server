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
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.TwinPublishConfigDTO;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class TwinConfigurationStore {

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

  Optional<DroneInfoDTO> getDrone(String name) {
    return config.getDroneInfo().stream()
        .filter(entry -> name.equals(entry.getName()))
        .findFirst();
  }

  void createDrone(DroneInfoDTO droneInfo) throws IOException {
    validateDrone(droneInfo);
    if (getDrone(droneInfo.getName()).isPresent()) {
      throw new TwinConfigurationException("Drone configuration already exists: " + droneInfo.getName(), 409);
    }
    config.getDroneInfo().add(droneInfo);
    config.save();
  }

  void updateDrone(String name, DroneInfoDTO droneInfo) throws IOException {
    validateName(name, "name");
    validateDrone(droneInfo);
    if (!name.equals(droneInfo.getName())) {
      throw new TwinConfigurationException("Drone configuration name cannot be changed", 400);
    }

    List<DroneInfoDTO> droneInfos = config.getDroneInfo();
    for (int index = 0; index < droneInfos.size(); index++) {
      if (name.equals(droneInfos.get(index).getName())) {
        droneInfos.set(index, droneInfo);
        config.save();
        return;
      }
    }
    throw new TwinConfigurationException("Unknown drone configuration: " + name, 404);
  }

  void deleteDrone(String name) throws IOException {
    validateName(name, "name");
    if (!config.getDroneInfo().removeIf(entry -> name.equals(entry.getName()))) {
      throw new TwinConfigurationException("Unknown drone configuration: " + name, 404);
    }
    config.save();
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

  private void validateName(String name, String fieldName) {
    if (name == null || name.isBlank()) {
      throw new TwinConfigurationException(fieldName + " is required", 400);
    }
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
