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
import io.mapsmessaging.state.config.capability.TaskCapability;
import io.mapsmessaging.state.config.capability.TaskSpecialization;
import io.mapsmessaging.state.config.geospatial.GeoSpatialAreaConfigDTO;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4UavModel;
import io.mapsmessaging.state.rest.twins.TwinConfigurationStore;
import io.mapsmessaging.state.rest.twins.TwinCoreConfigDTO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwinConfigurationStoreTest {

  @Test
  void core_update_persistsScalarConfiguration() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    TwinCoreConfigDTO coreConfig = new TwinCoreConfigDTO();
    coreConfig.setHeartbeatTimeoutMillis(7500);
    coreConfig.setStaleTimeoutMillis(15000);
    coreConfig.setRetentionTimeoutMillis(240000);
    coreConfig.setRemoveExpiredTwins(false);
    coreConfig.setDefaultRootPath("/drones");

    store.updateCoreConfig(coreConfig);

    assertEquals(7500, config.getHeartbeatTimeoutMillis());
    assertEquals(15000, config.getStaleTimeoutMillis());
    assertEquals(240000, config.getRetentionTimeoutMillis());
    assertFalse(config.isRemoveExpiredTwins());
    assertEquals("/drones", config.getDefaultRootPath());
    assertEquals(1, config.getSaveCount());
  }

  @Test
  void tak_putDelete_persistsEachMutation() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    TakProtocolDTO created = takConfig("tak.example.net", 8088);
    TakProtocolDTO updated = takConfig("tak2.example.net", 8089);

    config.setTak(null);
    store.putTakConfig(created);
    assertSame(created, store.getTakConfig().orElseThrow());

    store.putTakConfig(updated);
    assertSame(updated, store.getTakConfig().orElseThrow());

    store.deleteTakConfig();

    assertTrue(store.getTakConfig().isEmpty());
    assertEquals(3, config.getSaveCount());
  }

  @Test
  void publish_putDelete_persistsEachMutation() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    TwinPublishConfigDTO created = publishConfig("/state/twins/{twinId}");
    TwinPublishConfigDTO updated = publishConfig("/state/drones/{twinId}");

    config.setPublish(null);
    store.putPublishConfig(created);
    assertSame(created, store.getPublishConfig().orElseThrow());

    store.putPublishConfig(updated);
    assertSame(updated, store.getPublishConfig().orElseThrow());

    store.deletePublishConfig();

    assertTrue(store.getPublishConfig().isEmpty());
    assertEquals(3, config.getSaveCount());
  }

  @Test
  void n2k_updateDelete_persistsSpecificConfiguration() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    N2KTwinConfig n2kConfig = new N2KTwinConfig();
    n2kConfig.setTopic("/canbus1/n2k/json/#");
    n2kConfig.setName("canbus1");
    n2kConfig.setVehicleClass("USV");

    store.putN2kConfig(n2kConfig);
    assertSame(n2kConfig, store.getN2kConfig().orElseThrow());

    store.deleteN2kConfig();

    assertFalse(store.getN2kConfig().orElseThrow().isEnable());
    assertFalse(store.getN2kConfig().orElseThrow().isPublishMavlinkDrones());
    assertEquals(2, config.getSaveCount());
  }

  @Test
  void mavlink_createUpdateDelete_persistsEachMutation() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    MavlinkTwinConfigDTO created = mavlinkSource("primary", "/mavlink/>");
    MavlinkTwinConfigDTO updated = mavlinkSource("primary", "/mavlink/state/>");

    store.createMavlinkSource(created);
    assertSame(created, store.getMavlinkSource("primary").orElseThrow());

    store.updateMavlinkSource("primary", updated);
    assertSame(updated, store.getMavlinkSource("primary").orElseThrow());

    store.deleteMavlinkSource("primary");

    assertTrue(store.listMavlinkSources().isEmpty());
    assertEquals(3, config.getSaveCount());
  }

  @Test
  void mavlink_createDuplicate_returnsConflict() throws IOException {
    TwinConfigurationStore store = new TwinConfigurationStore(newConfig());

    store.createMavlinkSource(mavlinkSource("primary", "/mavlink/>"));
    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(
        TwinConfigurationStore.TwinConfigurationException.class,
        () -> store.createMavlinkSource(mavlinkSource("primary", "/other/>"))
    );

    assertEquals(409, exception.getStatusCode());
  }

  @Test
  void mavlink_createWithoutTopic_returnsBadRequest() {
    TwinConfigurationStore store = new TwinConfigurationStore(newConfig());

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(
        TwinConfigurationStore.TwinConfigurationException.class,
        () -> store.createMavlinkSource(mavlinkSource("primary", " "))
    );

    assertEquals(400, exception.getStatusCode());
  }

  @Test
  void drone_createUpdateDelete_persistsEachMutation() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    DroneInfoDTO created = drone("alpha");
    DroneInfoDTO updated = drone("alpha");
    updated.setUuid(created.getUuid());
    updated.setBatteryCapacityHours(24.0d);
    MavlinkTwinConfigDTO mavlinkSource = mavlinkSource("primary", "/mavlink/#");
    MavlinkTwinConfigDTO backupMavlinkSource = mavlinkSource("backup", "/mavlink/backup/#");
    mavlinkSource.setKnownSources(List.of(mavlinkKnownSource("alpha"), mavlinkKnownSource("bravo")));
    backupMavlinkSource.setKnownSources(List.of(mavlinkKnownSource("alpha")));
    config.getMavlink().addAll(List.of(mavlinkSource, backupMavlinkSource));

    store.createDrone(created);
    assertSame(created, store.getDrone("alpha").orElseThrow());

    store.updateDrone("alpha", updated);
    assertSame(updated, store.getDrone("alpha").orElseThrow());

    store.deleteDrone("alpha");

    assertTrue(store.listDrones().isEmpty());
    assertEquals(List.of("bravo"), mavlinkSource.getKnownSources().stream().map(MavlinkKnownSourceDTO::getName).toList());
    assertTrue(backupMavlinkSource.getKnownSources().isEmpty());
    assertEquals(3, config.getSaveCount());
  }

  @Test
  void drone_deleteUnknownName_returnsNotFoundWithoutSaving() {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(TwinConfigurationStore.TwinConfigurationException.class, () -> store.deleteDrone("missing"));

    assertEquals(404, exception.getStatusCode());
    assertEquals(0, config.getSaveCount());
  }

  @Test
  void drone_deleteInvalidName_returnsBadRequestWithoutSaving() {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(TwinConfigurationStore.TwinConfigurationException.class, () -> store.deleteDrone("invalid/name"));

    assertEquals(400, exception.getStatusCode());
    assertEquals(0, config.getSaveCount());
  }

  @Test
  void drone_deleteSaveFailure_restoresDroneAndMavlinkBindings() {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    DroneInfoDTO drone = drone("alpha");
    config.getDroneInfo().add(drone);
    MavlinkTwinConfigDTO firstSource = mavlinkSource("primary", "/mavlink/primary/#");
    MavlinkTwinConfigDTO secondSource = mavlinkSource("backup", "/mavlink/backup/#");
    List<MavlinkKnownSourceDTO> firstKnownSources = List.of(mavlinkKnownSource("alpha"), mavlinkKnownSource("bravo"));
    List<MavlinkKnownSourceDTO> secondKnownSources = List.of(mavlinkKnownSource("alpha"));
    firstSource.setKnownSources(firstKnownSources);
    secondSource.setKnownSources(secondKnownSources);
    config.getMavlink().addAll(List.of(firstSource, secondSource));
    config.failNextSave();

    assertThrows(IOException.class, () -> store.deleteDrone("alpha"));

    assertSame(drone, store.getDrone("alpha").orElseThrow());
    assertSame(firstKnownSources, firstSource.getKnownSources());
    assertSame(secondKnownSources, secondSource.getKnownSources());
    assertEquals(1, config.getSaveCount());
  }

  @Test
  void drone_createDuplicateNameIgnoringCase_returnsConflict() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);

    store.createDrone(drone("alpha"));
    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(
        TwinConfigurationStore.TwinConfigurationException.class,
        () -> store.createDrone(drone("ALPHA"))
    );

    assertEquals(409, exception.getStatusCode());
    assertEquals(1, store.listDrones().size());
    assertEquals(1, config.getSaveCount());
  }

  @Test
  void catalogues_returnRegisteredModelsAndConfiguredGeospatialAreas() {
    SavingTwinManagerConfig config = newConfig();
    GeoSpatialAreaConfigDTO bravo = new GeoSpatialAreaConfigDTO();
    bravo.setName("bravo");
    GeoSpatialAreaConfigDTO alpha = new GeoSpatialAreaConfigDTO();
    alpha.setName("alpha");
    config.getGeospatial().setAreas(List.of(bravo, alpha));
    TwinConfigurationStore store = new TwinConfigurationStore(config);

    assertTrue(store.listDroneModelNames().contains(GenericPx4UavModel.MODEL_NAME));
    assertEquals(List.of("alpha", "bravo"), store.listGeospatialAreaNames());
  }

  @Test
  void drone_createUnknownModel_returnsBadRequest() {
    TwinConfigurationStore store = new TwinConfigurationStore(newConfig());
    DroneInfoDTO drone = drone("alpha");
    drone.setModelName("unknown-model");

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(TwinConfigurationStore.TwinConfigurationException.class, () -> store.createDrone(drone));

    assertEquals(400, exception.getStatusCode());
    assertTrue(store.listDrones().isEmpty());
  }

  @Test
  void drone_createUnknownGeospatialArea_returnsBadRequest() {
    TwinConfigurationStore store = new TwinConfigurationStore(newConfig());
    DroneInfoDTO drone = drone("alpha");
    drone.setGeospatialArea("missing-area");

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(TwinConfigurationStore.TwinConfigurationException.class, () -> store.createDrone(drone));

    assertEquals(400, exception.getStatusCode());
    assertTrue(store.listDrones().isEmpty());
  }

  @Test
  void drone_createConfiguredGeospatialArea_persistsDrone() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    GeoSpatialAreaConfigDTO area = new GeoSpatialAreaConfigDTO();
    area.setName("test-area");
    config.getGeospatial().setAreas(List.of(area));
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    DroneInfoDTO drone = drone("alpha");
    drone.setGeospatialArea("test-area");

    store.createDrone(drone);

    assertSame(drone, store.getDrone("alpha").orElseThrow());
    assertEquals(1, config.getSaveCount());
  }

  @Test
  void drone_createDuplicateUuid_returnsConflict() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    DroneInfoDTO first = drone("alpha");
    DroneInfoDTO duplicate = drone("bravo");
    duplicate.setUuid(first.getUuid());

    store.createDrone(first);
    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(
        TwinConfigurationStore.TwinConfigurationException.class,
        () -> store.createDrone(duplicate)
    );

    assertEquals(409, exception.getStatusCode());
    assertEquals(1, store.listDrones().size());
    assertEquals(1, config.getSaveCount());
  }

  @Test
  void drone_createIncompleteConfiguration_returnsBadRequest() {
    TwinConfigurationStore store = new TwinConfigurationStore(newConfig());
    DroneInfoDTO incomplete = drone("alpha");
    incomplete.setModelName(" ");

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(
        TwinConfigurationStore.TwinConfigurationException.class,
        () -> store.createDrone(incomplete)
    );

    assertEquals(400, exception.getStatusCode());
    assertTrue(store.listDrones().isEmpty());
  }

  @Test
  void drone_createDuplicateTaskType_returnsBadRequest() {
    TwinConfigurationStore store = new TwinConfigurationStore(newConfig());
    DroneInfoDTO drone = drone("alpha");
    TaskCapability first = new TaskCapability(PlanTaskType.REPOSITION, TaskSpecialization.NONE, new Authorities[0]);
    TaskCapability duplicate = new TaskCapability(PlanTaskType.REPOSITION, TaskSpecialization.NONE, new Authorities[0]);
    drone.getCapabilities().setTasks(List.of(first, duplicate));

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(
        TwinConfigurationStore.TwinConfigurationException.class,
        () -> store.createDrone(drone)
    );

    assertEquals(400, exception.getStatusCode());
    assertTrue(store.listDrones().isEmpty());
  }

  @Test
  void drone_createInvalidNumericConfiguration_returnsBadRequest() {
    TwinConfigurationStore store = new TwinConfigurationStore(newConfig());
    DroneInfoDTO drone = drone("alpha");
    drone.setRangeMeters(0.0d);

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(
        TwinConfigurationStore.TwinConfigurationException.class,
        () -> store.createDrone(drone)
    );

    assertEquals(400, exception.getStatusCode());
    assertTrue(store.listDrones().isEmpty());
  }

  @Test
  void drone_createSaveFailure_restoresConfiguration() {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    config.failNextSave();

    assertThrows(IOException.class, () -> store.createDrone(drone("alpha")));

    assertTrue(store.listDrones().isEmpty());
    assertEquals(1, config.getSaveCount());
  }

  @Test
  void drone_updateChangingName_returnsBadRequest() {
    TwinConfigurationStore store = new TwinConfigurationStore(newConfig());

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(
        TwinConfigurationStore.TwinConfigurationException.class,
        () -> store.updateDrone("alpha", drone("bravo"))
    );

    assertEquals(400, exception.getStatusCode());
  }

  @Test
  void drone_updateMissingDrone_returnsNotFoundWithoutSaving() {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(TwinConfigurationStore.TwinConfigurationException.class, () -> store.updateDrone("alpha", drone("alpha")));

    assertEquals(404, exception.getStatusCode());
    assertEquals(0, config.getSaveCount());
  }

  @Test
  void drone_updateChangingUuid_returnsConflictWithoutSaving() {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    DroneInfoDTO existing = drone("alpha");
    config.getDroneInfo().add(existing);
    DroneInfoDTO updated = drone("alpha");

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(TwinConfigurationStore.TwinConfigurationException.class, () -> store.updateDrone("alpha", updated));

    assertEquals(409, exception.getStatusCode());
    assertSame(existing, store.getDrone("alpha").orElseThrow());
    assertEquals(0, config.getSaveCount());
  }

  @Test
  void drone_updateUuidConflictsWithAnotherDrone_returnsConflictWithoutSaving() {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    DroneInfoDTO existing = drone("alpha");
    DroneInfoDTO conflicting = drone("bravo");
    conflicting.setUuid(existing.getUuid());
    config.getDroneInfo().addAll(List.of(existing, conflicting));
    DroneInfoDTO updated = drone("alpha");
    updated.setUuid(existing.getUuid());

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(TwinConfigurationStore.TwinConfigurationException.class, () -> store.updateDrone("alpha", updated));

    assertEquals(409, exception.getStatusCode());
    assertSame(existing, store.getDrone("alpha").orElseThrow());
    assertEquals(0, config.getSaveCount());
  }

  @Test
  void drone_updateNameConflictsWithAnotherDrone_returnsConflictWithoutSaving() {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    DroneInfoDTO existing = drone("alpha");
    DroneInfoDTO conflicting = drone("ALPHA");
    config.getDroneInfo().addAll(List.of(existing, conflicting));
    DroneInfoDTO updated = drone("alpha");
    updated.setUuid(existing.getUuid());

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(TwinConfigurationStore.TwinConfigurationException.class, () -> store.updateDrone("alpha", updated));

    assertEquals(409, exception.getStatusCode());
    assertSame(existing, store.getDrone("alpha").orElseThrow());
    assertEquals(0, config.getSaveCount());
  }

  @Test
  void drone_updateInvalidConfiguration_returnsBadRequestWithoutSaving() {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    DroneInfoDTO existing = drone("alpha");
    config.getDroneInfo().add(existing);
    DroneInfoDTO updated = drone("alpha");
    updated.setUuid(existing.getUuid());
    updated.setModelName("unknown-model");

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(TwinConfigurationStore.TwinConfigurationException.class, () -> store.updateDrone("alpha", updated));

    assertEquals(400, exception.getStatusCode());
    assertSame(existing, store.getDrone("alpha").orElseThrow());
    assertEquals(0, config.getSaveCount());
  }

  @Test
  void drone_updateSaveFailure_restoresExistingConfiguration() {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    DroneInfoDTO existing = drone("alpha");
    config.getDroneInfo().add(existing);
    DroneInfoDTO updated = drone("alpha");
    updated.setUuid(existing.getUuid());
    updated.setBatteryCapacityHours(24.0d);
    config.failNextSave();

    assertThrows(IOException.class, () -> store.updateDrone("alpha", updated));

    assertSame(existing, store.getDrone("alpha").orElseThrow());
    assertEquals(1, config.getSaveCount());
  }

  @Test
  void drone_deleteMissing_returnsNotFound() {
    TwinConfigurationStore store = new TwinConfigurationStore(newConfig());

    TwinConfigurationStore.TwinConfigurationException exception = assertThrows(
        TwinConfigurationStore.TwinConfigurationException.class,
        () -> store.deleteDrone("missing")
    );

    assertEquals(404, exception.getStatusCode());
  }

  @Test
  void adapter_createUpdateDelete_persistsEachMutation() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    ConfigurationProperties created = adapterConfig("/source/one");
    ConfigurationProperties updated = adapterConfig("/source/two");

    store.createAdapterConfig("stanag", created);
    assertSame(created, store.getAdapterConfig("stanag").orElseThrow());

    store.updateAdapterConfig("stanag", updated);
    assertSame(updated, store.getAdapterConfig("stanag").orElseThrow());

    store.deleteAdapterConfig("stanag");

    assertTrue(store.getAdapterConfig("stanag").isEmpty());
    assertEquals(3, config.getSaveCount());
  }

  @Test
  void adapters_replaceAll_persistsMap() throws IOException {
    SavingTwinManagerConfig config = newConfig();
    TwinConfigurationStore store = new TwinConfigurationStore(config);
    Map<String, ConfigurationProperties> adapters = new LinkedHashMap<>();
    adapters.put("stanag", adapterConfig("/stanag/source"));
    adapters.put("custom", adapterConfig("/custom/source"));

    store.replaceAdapterConfigs(adapters);

    assertEquals(2, store.listAdapterConfigs().size());
    assertEquals("/stanag/source", store.getAdapterConfig("stanag").orElseThrow().getProperty("topic"));
    assertEquals(1, config.getSaveCount());
  }

  private SavingTwinManagerConfig newConfig() {
    SavingTwinManagerConfig config = new SavingTwinManagerConfig();
    config.setN2KTwinConfig(new N2KTwinConfig());
    return config;
  }

  private MavlinkTwinConfigDTO mavlinkSource(String name, String topic) {
    MavlinkTwinConfigDTO config = new MavlinkTwinConfigDTO();
    config.setName(name);
    config.setTopic(topic);
    config.setDialectName("common");
    return config;
  }

  private MavlinkKnownSourceDTO mavlinkKnownSource(String name) {
    MavlinkKnownSourceDTO source = new MavlinkKnownSourceDTO();
    source.setName(name);
    return source;
  }

  private DroneInfoDTO drone(String name) {
    DroneInfoDTO droneInfo = new DroneInfoDTO();
    droneInfo.setName(name);
    droneInfo.setUuid(UUID.randomUUID());
    droneInfo.setModelName(GenericPx4UavModel.MODEL_NAME);
    return droneInfo;
  }

  private TakProtocolDTO takConfig(String hostname, int port) {
    TakProtocolDTO takProtocolDTO = new TakProtocolDTO();
    takProtocolDTO.setHostname(hostname);
    takProtocolDTO.setPort(port);
    takProtocolDTO.setTopic("/tak/cot");
    return takProtocolDTO;
  }

  private TwinPublishConfigDTO publishConfig(String topicTemplate) {
    TwinPublishConfigDTO publishConfig = new TwinPublishConfigDTO();
    publishConfig.setEnabled(true);
    publishConfig.setTopicTemplate(topicTemplate);
    return publishConfig;
  }

  private ConfigurationProperties adapterConfig(String topic) {
    ConfigurationProperties adapterConfig = new ConfigurationProperties();
    adapterConfig.put("topic", topic);
    adapterConfig.put("enabled", true);
    return adapterConfig;
  }

  private static class SavingTwinManagerConfig extends TwinManagerConfig {

    private int saveCount;
    private boolean failNextSave;

    @Override
    public void save() throws IOException {
      saveCount++;
      if (failNextSave) {
        failNextSave = false;
        throw new IOException("Expected test save failure");
      }
    }

    void failNextSave() {
      failNextSave = true;
    }

    int getSaveCount() {
      return saveCount;
    }
  }
}
