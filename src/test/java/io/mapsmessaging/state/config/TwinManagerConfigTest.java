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

package io.mapsmessaging.state.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.state.config.capability.TaskCapabilities;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TwinManagerConfigTest {

  @Test
  void dto_adapterConfig_defaultsEmptyAndAcceptsMultipleAdapterKeys() {
    TwinManagerConfigDTO config = new TwinManagerConfigDTO();
    ConfigurationProperties firstAdapter = adapterProperties("first/topic", 11);
    ConfigurationProperties secondAdapter = adapterProperties("second/topic", 22);

    config.getAdapterConfig().put("alphaAdapter", firstAdapter);
    config.getAdapterConfig().put("bravoAdapter", secondAdapter);

    assertEquals(2, config.getAdapterConfig().size());
    assertSame(firstAdapter, config.getAdapterConfig().get("alphaAdapter"));
    assertSame(secondAdapter, config.getAdapterConfig().get("bravoAdapter"));
  }

  @Test
  void constructor_preservesArbitraryAdapterBlocks() throws ReflectiveOperationException {
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("heartbeatTimeoutMillis", 5000L);
    root.put("stateAdapters", stateAdapterConfig(
        "alphaAdapter", adapterProperties("alpha/topic", 11),
        "bravoAdapter", adapterProperties("bravo/topic", 22)
    ));

    TwinManagerConfig config = newTwinManagerConfig(root);

    assertEquals(2, config.getAdapterConfig().size());
    assertEquals("alpha/topic", config.getAdapterConfig().get("alphaAdapter").getProperty("topic"));
    assertEquals(11, config.getAdapterConfig().get("alphaAdapter").getIntProperty("interval", 0));
    assertEquals("bravo/topic", config.getAdapterConfig().get("bravoAdapter").getProperty("topic"));
    assertEquals(22, config.getAdapterConfig().get("bravoAdapter").getIntProperty("interval", 0));
  }

  @Test
  void constructor_ignoresTopLevelAdapterLikeBlocks() throws ReflectiveOperationException {
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("alphaAdapter", adapterProperties("alpha/topic", 5));

    TwinManagerConfig config = newTwinManagerConfig(root);

    assertTrue(config.getAdapterConfig().isEmpty());
  }

  @Test
  void toConfigurationProperties_writesAllAdapterBlocksBack() throws ReflectiveOperationException {
    ConfigurationProperties root = new ConfigurationProperties();
    ConfigurationProperties firstAdapter = adapterProperties("alpha/topic", 11);
    ConfigurationProperties secondAdapter = adapterProperties("bravo/topic", 22);
    root.put("stateAdapters", stateAdapterConfig(
        "alphaAdapter", firstAdapter,
        "bravoAdapter", secondAdapter
    ));

    TwinManagerConfig config = newTwinManagerConfig(root);
    ConfigurationProperties saved = config.toConfigurationProperties();
    ConfigurationProperties savedAdapters = assertInstanceOf(ConfigurationProperties.class, saved.get("stateAdapters"));

    assertSame(firstAdapter, savedAdapters.get("alphaAdapter"));
    assertSame(secondAdapter, savedAdapters.get("bravoAdapter"));
    assertNull(saved.get("alphaAdapter"));
    assertNull(saved.get("bravoAdapter"));
  }

  @Test
  void toConfigurationProperties_writesDroneInfoBack() throws ReflectiveOperationException {
    UUID droneUuid = UUID.randomUUID();
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("droneInfo", List.of(droneInfoProperties("drone-alpha", droneUuid)));

    TwinManagerConfig config = newTwinManagerConfig(root);
    ConfigurationProperties saved = config.toConfigurationProperties();
    List<?> savedDroneInfos = assertInstanceOf(List.class, saved.get("droneInfo"));
    ConfigurationProperties savedDroneInfo = assertInstanceOf(ConfigurationProperties.class, savedDroneInfos.get(0));

    assertEquals("drone-alpha", savedDroneInfo.getProperty("name"));
    assertEquals(droneUuid.toString(), savedDroneInfo.getProperty("uuid"));
    assertInstanceOf(ConfigurationProperties.class, savedDroneInfo.get("description"));
    assertInstanceOf(ConfigurationProperties.class, savedDroneInfo.get("capabilities"));
  }

  @Test
  void toConfigurationProperties_writesN2kFieldsBack() throws ReflectiveOperationException {
    ConfigurationProperties root = new ConfigurationProperties();
    ConfigurationProperties n2k = new ConfigurationProperties();
    n2k.put("enabled", true);
    n2k.put("topic", "/canbus1/n2k/json/#");
    n2k.put("name", "canbus1");
    n2k.put("vehicleClass", "USV");
    n2k.put("publishMavlinkDrones", false);
    root.put("n2k", n2k);

    TwinManagerConfig config = newTwinManagerConfig(root);
    ConfigurationProperties saved = config.toConfigurationProperties();
    ConfigurationProperties savedN2k = assertInstanceOf(ConfigurationProperties.class, saved.get("n2k"));

    assertEquals(true, savedN2k.get("enabled"));
    assertEquals("/canbus1/n2k/json/#", savedN2k.getProperty("topic"));
    assertEquals("canbus1", savedN2k.getProperty("name"));
    assertEquals("USV", savedN2k.getProperty("vehicleClass"));
    assertEquals(false, savedN2k.get("publishMavlinkDrones"));
  }

  @Test
  void toConfigurationProperties_omitsDefaultDisabledN2kBlock() throws ReflectiveOperationException {
    ConfigurationProperties root = new ConfigurationProperties();

    TwinManagerConfig config = newTwinManagerConfig(root);
    ConfigurationProperties saved = config.toConfigurationProperties();

    assertNull(saved.get("n2k"));
  }

  @Test
  void constructor_allowsDroneInfoWithoutDescription() throws ReflectiveOperationException {
    UUID droneUuid = UUID.randomUUID();
    ConfigurationProperties root = new ConfigurationProperties();
    ConfigurationProperties droneInfo = new ConfigurationProperties();
    droneInfo.put("name", "drone-alpha");
    droneInfo.put("uuid", droneUuid.toString());
    root.put("droneInfo", List.of(droneInfo));

    TwinManagerConfig config = newTwinManagerConfig(root);

    assertEquals(1, config.getDroneInfo().size());
    assertEquals("drone-alpha", config.getDroneInfo().get(0).getName());
    assertEquals(droneUuid, config.getDroneInfo().get(0).getUuid());
    assertNull(config.getDroneInfo().get(0).getDescription());
    assertInstanceOf(TaskCapabilities.class, config.getDroneInfo().get(0).getCapabilities());
  }

  @Test
  void update_copiesAdapterConfigMap() {
    TwinManagerConfig config = new TwinManagerConfig();
    config.setN2KTwinConfig(new N2KTwinConfig());
    TwinManagerConfigDTO newConfig = new TwinManagerConfigDTO();
    newConfig.setN2KTwinConfig(new N2KTwinConfig());
    ConfigurationProperties adapter = adapterProperties("alpha/topic", 11);
    newConfig.getAdapterConfig().put("alphaAdapter", adapter);

    boolean changed = config.update(newConfig);

    assertTrue(changed);
    assertEquals(1, config.getAdapterConfig().size());
    assertSame(adapter, config.getAdapterConfig().get("alphaAdapter"));
  }

  @Test
  void constructor_readsConfigurationWrittenByToConfigurationProperties() throws ReflectiveOperationException {
    UUID droneUuid = UUID.randomUUID();
    TwinManagerConfig original = new TwinManagerConfig();
    original.setHeartbeatTimeoutMillis(7000);
    original.setStaleTimeoutMillis(14000);
    original.setRetentionTimeoutMillis(28000);
    original.setRemoveExpiredTwins(false);
    original.setDefaultRootPath("/it");

    TakProtocolDTO tak = new TakProtocolDTO();
    tak.setHostname("127.0.0.1");
    tak.setPort(8088);
    tak.setSharedConnection(true);
    tak.setTopic("tak/events");
    original.setTak(tak);

    TwinPublishConfigDTO publish = new TwinPublishConfigDTO();
    publish.setEnabled(true);
    publish.setTopicTemplate("/it/twins/{twinId}");
    original.setPublish(publish);

    N2KTwinConfig n2k = new N2KTwinConfig();
    n2k.setEnable(true);
    n2k.setPublishMavlinkDrones(false);
    n2k.setTopic("/it/n2k/json/#");
    n2k.setName("it-n2k");
    n2k.setVehicleClass("USV");
    original.setN2KTwinConfig(n2k);

    MavlinkTwinConfigDTO mavlink = new MavlinkTwinConfigDTO();
    mavlink.setName("it-mavlink");
    mavlink.setTopic("/it/mavlink/#");
    mavlink.setDialectName("common");
    original.getMavlink().add(mavlink);
    original.getDroneInfo().add(droneInfo("it-drone", droneUuid));
    original.getAdapterConfig().put("itAdapter", adapterProperties("/it/adapter", 15));

    TwinManagerConfig reloaded = newTwinManagerConfig(original.toConfigurationProperties());

    assertEquals(7000, reloaded.getHeartbeatTimeoutMillis());
    assertEquals(14000, reloaded.getStaleTimeoutMillis());
    assertEquals(28000, reloaded.getRetentionTimeoutMillis());
    assertEquals(false, reloaded.isRemoveExpiredTwins());
    assertEquals("/it", reloaded.getDefaultRootPath());
    assertEquals("127.0.0.1", reloaded.getTak().getHostname());
    assertEquals(8088, reloaded.getTak().getPort());
    assertEquals(true, reloaded.getTak().isSharedConnection());
    assertEquals("tak/events", reloaded.getTak().getTopic());
    assertEquals(true, reloaded.getPublish().isEnabled());
    assertEquals("/it/twins/{twinId}", reloaded.getPublish().getTopicTemplate());
    assertEquals(true, reloaded.getN2KTwinConfig().isEnable());
    assertEquals(false, reloaded.getN2KTwinConfig().isPublishMavlinkDrones());
    assertEquals("/it/n2k/json/#", reloaded.getN2KTwinConfig().getTopic());
    assertEquals("it-n2k", reloaded.getN2KTwinConfig().getName());
    assertEquals("USV", reloaded.getN2KTwinConfig().getVehicleClass());
    assertEquals(1, reloaded.getMavlink().size());
    assertEquals("it-mavlink", reloaded.getMavlink().get(0).getName());
    assertEquals("/it/mavlink/#", reloaded.getMavlink().get(0).getTopic());
    assertEquals("common", reloaded.getMavlink().get(0).getDialectName());
    assertEquals(1, reloaded.getDroneInfo().size());
    assertEquals("it-drone", reloaded.getDroneInfo().get(0).getName());
    assertEquals(droneUuid, reloaded.getDroneInfo().get(0).getUuid());
    assertEquals(1, reloaded.getAdapterConfig().size());
    assertEquals("/it/adapter", reloaded.getAdapterConfig().get("itAdapter").getProperty("topic"));
  }

  private ConfigurationProperties adapterProperties(String topic, int interval) {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", true);
    properties.put("topic", topic);
    properties.put("interval", interval);
    return properties;
  }

  private ConfigurationProperties stateAdapterConfig(
      String firstName,
      ConfigurationProperties firstProperties,
      String secondName,
      ConfigurationProperties secondProperties
  ) {
    ConfigurationProperties stateAdapters = new ConfigurationProperties();
    stateAdapters.put(firstName, firstProperties);
    stateAdapters.put(secondName, secondProperties);
    return stateAdapters;
  }

  private ConfigurationProperties droneInfoProperties(String name, UUID uuid) {
    ConfigurationProperties properties = new ConfigurationProperties();
    ConfigurationProperties description = new ConfigurationProperties();
    description.put("manufacturer", "MapsMessaging");
    properties.put("name", name);
    properties.put("uuid", uuid.toString());
    properties.put("description", description);
    properties.put("capabilities", new ConfigurationProperties());
    return properties;
  }

  private DroneInfo droneInfo(String name, UUID uuid) {
    DroneInfo droneInfo = new DroneInfo();
    droneInfo.setName(name);
    droneInfo.setUuid(uuid);
    droneInfo.setCapabilities(new TaskCapabilities());
    return droneInfo;
  }

  private TwinManagerConfig newTwinManagerConfig(ConfigurationProperties properties)
      throws ReflectiveOperationException {
    Constructor<TwinManagerConfig> constructor = TwinManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }
}
