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

package io.mapsmessaging.hardware.device.handler;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeviceHandlerTest {

  @Test
  void getTopicName_replacesAllPlaceholdersAndNormalisesSeparators() {
    DeviceController controller = mock(DeviceController.class);
    when(controller.getName()).thenReturn("weather");
    when(controller.getType()).thenReturn(DeviceType.SENSOR);
    DeviceHandler handler = new TestDeviceHandler(controller, 2, 26);

    String topic = handler.getTopicName(
        "/devices//[bus_name]/[bus_number]/[device_addr]/[device_name]/[device_type]/[device_name]");

    assertEquals("/devices/testBus/2/0x1a/weather/sensor/weather", topic);
  }

  @Test
  void getTopicName_omitsUnavailableBusNumberAndAddress() {
    DeviceController controller = mock(DeviceController.class);
    when(controller.getName()).thenReturn("clock");
    when(controller.getType()).thenReturn(DeviceType.CLOCK);
    DeviceHandler handler = new TestDeviceHandler(controller, -1, -1);

    String topic = handler.getTopicName("[bus_name]/[bus_number]/[device_addr]/[device_name]/[device_type]");

    assertEquals("testBus/clock/clock", topic);
  }

  private static final class TestDeviceHandler extends DeviceHandler {

    private final int busNumber;
    private final int deviceAddress;

    private TestDeviceHandler(DeviceController controller, int busNumber, int deviceAddress) {
      super("key", controller);
      this.busNumber = busNumber;
      this.deviceAddress = deviceAddress;
    }

    @Override
    public String getBusName() {
      return "testBus";
    }

    @Override
    public int getBusNumber() {
      return busNumber;
    }

    @Override
    public int getDeviceAddress() {
      return deviceAddress;
    }
  }
}
