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

package io.mapsmessaging.config.destination;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.license.FeatureManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

class DestinationConfigTest {

  @Test
  void configurationRoundTrip_preservesMessageOverridesBlock() {
    ConfigurationProperties overrideProperties = new ConfigurationProperties();
    overrideProperties.put("expiry", 5000L);
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("directory", "./target/override-config");
    properties.put("namespace", "/fleet/drone");
    properties.put("type", "Memory");
    properties.put("messageOverrides", overrideProperties);
    DestinationConfig original = new DestinationConfig(properties, new FeatureManager(new ArrayList<>()));

    ConfigurationProperties saved = original.toConfigurationProperties();
    DestinationConfig reloaded = new DestinationConfig(saved, new FeatureManager(new ArrayList<>()));

    Assertions.assertTrue(saved.containsKey("messageOverrides"));
    Assertions.assertFalse(saved.containsKey("messageOverride"));
    Assertions.assertNotNull(reloaded.getMessageOverride());
    Assertions.assertEquals(5000L, reloaded.getMessageOverride().getExpiry());
  }

  @Test
  void legacySingularMessageOverrideBlock_isStillAccepted() {
    ConfigurationProperties overrideProperties = new ConfigurationProperties();
    overrideProperties.put("expiry", 5000L);
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("directory", "./target/override-config");
    properties.put("namespace", "/fleet/drone");
    properties.put("type", "Memory");
    properties.put("messageOverride", overrideProperties);

    DestinationConfig config = new DestinationConfig(properties, new FeatureManager(new ArrayList<>()));

    Assertions.assertNotNull(config.getMessageOverride());
    Assertions.assertEquals(5000L, config.getMessageOverride().getExpiry());
  }
}
