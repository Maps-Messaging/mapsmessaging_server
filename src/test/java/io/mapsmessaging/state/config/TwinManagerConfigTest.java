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
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import java.lang.reflect.Constructor;
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

  private TwinManagerConfig newTwinManagerConfig(ConfigurationProperties properties)
      throws ReflectiveOperationException {
    Constructor<TwinManagerConfig> constructor = TwinManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }
}
