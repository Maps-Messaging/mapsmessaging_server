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

package io.mapsmessaging.config.network;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SerialDeviceHelperTest {

  @Test
  void getSerialDeviceDTO_readsSerialSettingsFromNestedBlockAndIoSettingsFromParent() {
    ConfigurationProperties serial = new ConfigurationProperties();
    serial.put("port", "COM7");
    serial.put("baudRate", 115200);
    serial.put("dataBits", 7);
    serial.put("stopBits", 2.0f);
    serial.put("parity", "EVEN");
    serial.put("flowControl", 3);
    serial.put("serialNo", "device-42");

    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("serial", serial);
    properties.put("readTimeOut", 101);
    properties.put("writeTimeOut", 202);
    properties.put("bufferSize", 4096);

    SerialDeviceDTO result = SerialDeviceHelper.getSerialDeviceDTO(properties);

    assertEquals("COM7", result.getPort());
    assertEquals(115200, result.getBaudRate());
    assertEquals(7, result.getDataBits());
    assertEquals(2.0f, result.getStopBits());
    assertEquals("EVEN", result.getParity());
    assertEquals(3, result.getFlowControl());
    assertEquals("device-42", result.getSerialNo());
    assertEquals(101, result.getReadTimeOut());
    assertEquals(202, result.getWriteTimeOut());
    assertEquals(4096, result.getBufferSize());
  }

  @Test
  void getSerialDeviceDTO_readsFlatConfiguration() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("port", "/dev/ttyUSB0");
    properties.put("baudRate", 57600);

    SerialDeviceDTO result = SerialDeviceHelper.getSerialDeviceDTO(properties);

    assertEquals("/dev/ttyUSB0", result.getPort());
    assertEquals(57600, result.getBaudRate());
  }
}
