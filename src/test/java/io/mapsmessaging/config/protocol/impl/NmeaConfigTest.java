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

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.NmeaConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NmeaConfigTest {

  @Test
  void emptyConfigurationUsesDtoDefaults() {
    NmeaConfig config = new NmeaConfig(new ConfigurationProperties());

    assertEquals("NMEA-0183", config.getType());
    assertEquals("json", config.getFormat());
    assertEquals("/NMEA0183/{deviceName}/{sentence}", config.getTopicNameTemplate());
    assertTrue(config.isPublish());
    assertFalse(config.isUseForServerLocation());
    assertEquals("GGA", config.getSentenceForServerLocation());
    assertEquals(0, config.getQualityOfService());
    assertFalse(config.isStoreOffline());
    assertNotNull(config.getSerial());
  }

  @Test
  void configuredValuesRoundTripThroughConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("format", "json");
    properties.put("topicNameTemplate", "/nav/{deviceName}/{sentence}");
    properties.put("publish", false);
    properties.put("useForServerLocation", true);
    properties.put("sentenceForServerLocation", "GPRMC");
    properties.put("qualityOfService", 2);
    properties.put("storeOffline", true);
    properties.put("port", "/dev/ttyUSB0");
    properties.put("baudRate", 38400);

    NmeaConfig config = new NmeaConfig(properties);
    ConfigurationProperties packed = config.toConfigurationProperties();
    NmeaConfig restored = new NmeaConfig(packed);

    assertEquals("/nav/{deviceName}/{sentence}", restored.getTopicNameTemplate());
    assertFalse(restored.isPublish());
    assertTrue(restored.isUseForServerLocation());
    assertEquals("GPRMC", restored.getSentenceForServerLocation());
    assertEquals(2, restored.getQualityOfService());
    assertTrue(restored.isStoreOffline());
    assertNotNull(restored.getSerial());
  }

  @Test
  void updateReportsOnlyActualChanges() {
    NmeaConfig config = new NmeaConfig(new ConfigurationProperties());

    NmeaConfigDTO updated = new NmeaConfigDTO();
    updated.setFormat("json-v2");
    updated.setTopicNameTemplate("/nmea/{sentence}");
    updated.setPublish(false);
    updated.setUseForServerLocation(true);
    updated.setSentenceForServerLocation("RMC");
    updated.setQualityOfService(1);
    updated.setStoreOffline(true);

    assertTrue(config.update(updated));
    assertEquals("json-v2", config.getFormat());
    assertEquals("/nmea/{sentence}", config.getTopicNameTemplate());
    assertFalse(config.isPublish());
    assertTrue(config.isUseForServerLocation());
    assertEquals("RMC", config.getSentenceForServerLocation());
    assertEquals(1, config.getQualityOfService());
    assertTrue(config.isStoreOffline());

    assertFalse(config.update(updated));
    assertFalse(config.update(new CotProtocolConfigDTO()));
  }
}
