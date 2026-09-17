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
import io.mapsmessaging.dto.rest.config.device.I2CDeviceConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SerialBusDeviceDTO;
import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerialDeviceConfigTest {

  @Test
  void serialDevice_roundTrip_preservesConfiguration() {
    SerialDeviceConfig original = new SerialDeviceConfig(serialProperties());

    ConfigurationProperties serialized = original.toConfigurationProperties();

    assertNotNull(serialized);
    assertEquals("SEN0657", serialized.getProperty("name"));
    assertEquals("temperature > 20", serialized.getProperty("selector"));

    SerialDeviceConfig roundTripped = new SerialDeviceConfig(serialized);
    assertEquals("SEN0657", roundTripped.getName());
    assertEquals("temperature > 20", roundTripped.getSelector());
    assertEquals("/dev/ttyAMA0", roundTripped.getSerialConfig().getPort());
    assertEquals(4_800, roundTripped.getSerialConfig().getBaudRate());
    assertEquals(8, roundTripped.getSerialConfig().getDataBits());
    assertEquals(1.0f, roundTripped.getSerialConfig().getStopBits());
    assertEquals("n", roundTripped.getSerialConfig().getParity());
    assertEquals(1, roundTripped.getSerialConfig().getFlowControl());
  }

  @Test
  void update_serialDeviceDto_appliesChanges() {
    SerialDeviceConfig config = new SerialDeviceConfig(serialProperties());
    SerialBusDeviceDTO update = new SerialBusDeviceDTO();
    update.setName("SEN0640");
    update.setSelector("humidity > 30");

    SerialDeviceDTO serial = new SerialDeviceDTO();
    serial.setPort("/dev/ttyAMA1");
    serial.setBaudRate(9_600);
    serial.setDataBits(7);
    serial.setStopBits(2.0f);
    serial.setParity("e");
    serial.setFlowControl(0);
    update.setSerialConfig(serial);

    assertTrue(config.update(update));
    assertEquals("SEN0640", config.getName());
    assertEquals("humidity > 30", config.getSelector());
    assertEquals("/dev/ttyAMA1", config.getSerialConfig().getPort());
    assertEquals(9_600, config.getSerialConfig().getBaudRate());
  }

  @Test
  void update_sameSerialDeviceDto_returnsFalse() {
    SerialDeviceConfig config = new SerialDeviceConfig(serialProperties());
    SerialBusDeviceDTO update = new SerialBusDeviceDTO();
    update.setName(config.getName());
    update.setSelector(config.getSelector());
    update.setSerialConfig(config.getSerialConfig());
    update.setReadTimeOut(config.getReadTimeOut());
    update.setWriteTimeOut(config.getWriteTimeOut());

    assertFalse(config.update(update));
  }

  @Test
  void update_unrelatedDto_returnsFalse() {
    SerialDeviceConfig config = new SerialDeviceConfig(serialProperties());

    assertFalse(config.update(new I2CDeviceConfigDTO()));
  }

  private ConfigurationProperties serialProperties() {
    ConfigurationProperties serial = new ConfigurationProperties();
    serial.put("port", "/dev/ttyAMA0");
    serial.put("baudRate", 4_800);
    serial.put("dataBits", 8);
    serial.put("stopBits", 1.0f);
    serial.put("parity", "n");
    serial.put("flowControl", 1);

    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "SEN0657");
    properties.put("selector", "temperature > 20");
    properties.put("serial", serial);
    return properties;
  }
}
