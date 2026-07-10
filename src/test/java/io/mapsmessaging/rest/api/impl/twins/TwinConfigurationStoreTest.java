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

package io.mapsmessaging.rest.api.impl.twins;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.state.config.DroneInfo;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.TwinPublishConfigDTO;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
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
    DroneInfo created = drone("alpha");
    DroneInfo updated = drone("alpha");
    updated.setUuid(UUID.randomUUID());

    store.createDrone(created);
    assertSame(created, store.getDrone("alpha").orElseThrow());

    store.updateDrone("alpha", updated);
    assertSame(updated, store.getDrone("alpha").orElseThrow());

    store.deleteDrone("alpha");

    assertTrue(store.listDrones().isEmpty());
    assertEquals(3, config.getSaveCount());
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

  private DroneInfo drone(String name) {
    DroneInfo droneInfo = new DroneInfo();
    droneInfo.setName(name);
    droneInfo.setUuid(UUID.randomUUID());
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

    @Override
    public void save() {
      saveCount++;
    }

    int getSaveCount() {
      return saveCount;
    }
  }
}
