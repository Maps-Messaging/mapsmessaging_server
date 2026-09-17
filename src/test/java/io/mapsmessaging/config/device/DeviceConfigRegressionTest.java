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

package io.mapsmessaging.config.device;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.device.SerialBusConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceConfigRegressionTest {

  @Test
  void serialBusUpdateAcceptsSerialBusDto() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "serial-a");
    properties.put("enabled", true);
    properties.put("topicNameTemplate", "/serial/[device_name]");
    properties.put("filter", "ON_CHANGE");
    properties.put("selector", "");

    SerialDeviceBusConfig config = new SerialDeviceBusConfig(properties);

    SerialBusConfigDTO update = new SerialBusConfigDTO();
    update.setName("serial-b");
    update.setEnabled(false);
    update.setTopicNameTemplate("/updated/[device_name]");
    update.setFilter("ALWAYS_SEND");
    update.setSelector("temperature > 20");

    assertTrue(config.update(update));
    assertEquals("serial-b", config.getName());
    assertEquals("/updated/[device_name]", config.getTopicNameTemplate());
    assertEquals("ALWAYS_SEND", config.getFilter());
    assertEquals("temperature > 20", config.getSelector());
  }

  @Test
  void spiBusRoundTripPreservesConfiguredDevices() {
    ConfigurationProperties device = new ConfigurationProperties();
    device.put("name", "Mcp3y0x");
    device.put("spiBus", 0);
    device.put("spiMode", 0);
    device.put("spiChipSelect", 1);
    device.put("address", 12);

    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "spi");
    properties.put("enabled", true);
    properties.put("config", List.of(device));

    SpiDeviceBusConfig original = new SpiDeviceBusConfig(properties);
    assertEquals(1, original.getDevices().size());

    ConfigurationProperties serialized = original.toConfigurationProperties();
    assertTrue(serialized.containsKey("config"));

    SpiDeviceBusConfig roundTripped = new SpiDeviceBusConfig(serialized);
    assertEquals(1, roundTripped.getDevices().size());
    assertEquals("Mcp3y0x", roundTripped.getDevices().get(0).getName());
  }

  @Test
  void i2cDeviceAcceptsHexAddressUsedByConfiguration() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("address", "0x10");
    properties.put("name", "DebugDevice");

    I2CDeviceConfig config = new I2CDeviceConfig(properties);

    assertEquals(16, config.getAddress());
    assertEquals("DebugDevice", config.getName());
  }
}
