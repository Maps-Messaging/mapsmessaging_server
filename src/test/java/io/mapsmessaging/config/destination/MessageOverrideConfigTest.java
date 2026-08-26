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

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MessageOverrideConfigTest {

  @Test
  void omittedPriority_remainsUnset() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("expiry", 5000L);

    MessageOverrideConfig config = new MessageOverrideConfig(properties);

    Assertions.assertNull(config.getPriority());
  }

  @Test
  void configurationRoundTrip_preservesCanonicalKeysAndValues() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("expiry", 5000L);
    properties.put("qos", QualityOfService.AT_LEAST_ONCE.name());
    properties.put("storeOffline", true);

    MessageOverrideConfig original = new MessageOverrideConfig(properties);
    ConfigurationProperties saved = original.toConfigurationProperties();
    MessageOverrideConfig reloaded = new MessageOverrideConfig(saved);

    Assertions.assertTrue(saved.containsKey("qos"));
    Assertions.assertFalse(saved.containsKey("qualityOfService"));
    Assertions.assertEquals(5000L, reloaded.getExpiry());
    Assertions.assertEquals(QualityOfService.AT_LEAST_ONCE, reloaded.getQualityOfService());
    Assertions.assertTrue(reloaded.getStoreOffline());
  }

  @Test
  void legacyQualityOfServiceKey_isStillAccepted() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("qualityOfService", QualityOfService.EXACTLY_ONCE.name());

    MessageOverrideConfig config = new MessageOverrideConfig(properties);

    Assertions.assertEquals(QualityOfService.EXACTLY_ONCE, config.getQualityOfService());
  }
}
