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

package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigHelperTest {

  @Test
  void parseBufferSize_acceptsUnitsWhitespaceCaseAndDecimalBytes() {
    assertEquals(2L * 1024, ConfigHelper.parseBufferSize(" 2k "));
    assertEquals(3L * 1024 * 1024, ConfigHelper.parseBufferSize("3M"));
    assertEquals(4L * 1024 * 1024 * 1024, ConfigHelper.parseBufferSize("4g"));
    assertEquals(512, ConfigHelper.parseBufferSize("512.75"));
  }

  @Test
  void parseBufferSize_rejectsMalformedValues() {
    assertThrows(NumberFormatException.class, () -> ConfigHelper.parseBufferSize("1.5K"));
    assertThrows(NumberFormatException.class, () -> ConfigHelper.parseBufferSize("ten"));
  }

  @Test
  void formatBufferSize_usesLargestWholeUnitThreshold() {
    assertEquals("1023", ConfigHelper.formatBufferSize(1023));
    assertEquals("1K", ConfigHelper.formatBufferSize(1024));
    assertEquals("1M", ConfigHelper.formatBufferSize(1024L * 1024));
    assertEquals("1G", ConfigHelper.formatBufferSize(1024L * 1024 * 1024));
    assertEquals("1K", ConfigHelper.formatBufferSize(1536));
  }

  @Test
  void buildMap_convertsNestedConfigurationPropertiesAndHandlesNull() {
    ConfigurationProperties nested = new ConfigurationProperties();
    nested.put("enabled", true);
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "server");
    properties.put("nested", nested);

    Map<String, Object> result = ConfigHelper.buildMap(properties);

    assertEquals("server", result.get("name"));
    Map<?, ?> nestedResult = assertInstanceOf(Map.class, result.get("nested"));
    assertEquals("true", nestedResult.get("enabled"));
    assertTrue(ConfigHelper.buildMap(null).isEmpty());
  }

  @Test
  void updateMap_onlyAddsOrChangesEntriesAndPreservesUnmentionedKeys() {
    Map<String, Object> current = new LinkedHashMap<>();
    current.put("unchanged", 1);
    current.put("updated", "old");
    current.put("preserved", true);

    assertTrue(ConfigHelper.updateMap(current, Map.of("unchanged", 1, "updated", "new", "added", 2)));
    assertEquals(Map.of("unchanged", 1, "updated", "new", "preserved", true, "added", 2), current);
    assertFalse(ConfigHelper.updateMap(current, Map.of("unchanged", 1, "updated", "new", "added", 2)));
  }
}
