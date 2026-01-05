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

package io.mapsmessaging.hardware.device.handler.spi;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.spi.SpiBusManager;
import io.mapsmessaging.devices.spi.SpiDeviceController;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceConfigDTO;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;

import java.util.Map;

public class SpiBusHandler extends BusHandler {

  private final SpiBusManager spiBusManager;

  public SpiBusHandler(SpiBusManager spiBusManager, SpiDeviceBusConfigDTO spiDeviceBusConfigDTO, Trigger trigger) {
    super(spiDeviceBusConfigDTO, trigger);
    this.spiBusManager = spiBusManager;
  }

  @Override
  protected Map<String, DeviceController> scan() {
    Map<String, DeviceController> map = spiBusManager.getActive();
    SpiDeviceBusConfigDTO config = (SpiDeviceBusConfigDTO)properties;
    for(SpiDeviceConfigDTO deviceConfig: config.getDevices()){
      if(!map.containsKey(deviceConfig.getName())){
        SpiDeviceController controller = null;
        try {
          controller = spiBusManager.configureDevice(deviceConfig.getName(), deviceConfig.getConfig());
        } catch (Exception e) {
          // ToDo log this
        }
        map.put(deviceConfig.getName(), controller);
      }
    }
    return map;
  }

  @Override
  protected DeviceHandler createDeviceHander(String key, DeviceController controller) {
    return new SpiDeviceHandler(key, controller);
  }


}