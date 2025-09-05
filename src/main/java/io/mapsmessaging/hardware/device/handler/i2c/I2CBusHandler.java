/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.hardware.device.handler.i2c;

import io.mapsmessaging.config.device.I2CBusConfig;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.i2c.I2CBusManager;
import io.mapsmessaging.devices.i2c.I2CDeviceController;
import io.mapsmessaging.dto.rest.config.device.I2CBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.I2CDeviceConfigDTO;
import io.mapsmessaging.hardware.device.DeviceSessionManagement;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class I2CBusHandler extends BusHandler {

  private final I2CBusManager i2CBusManager;
  private final Map<Integer, String> selectorMap;

  public I2CBusHandler(I2CBusManager i2CBusManager, I2CBusConfigDTO properties, Trigger trigger){
    super(properties, trigger);
    this.i2CBusManager = i2CBusManager;
    selectorMap = new LinkedHashMap<>();
    processConfiguredDevices();
  }

  @Override
  protected DeviceHandler createDeviceHander(String key, DeviceController controller) {
    return new I2CDeviceHandler(key, controller);
  }

  @Override
  public void closedSession(DeviceSessionManagement deviceSessionManagement){
    i2CBusManager.close((I2CDeviceController) deviceSessionManagement.getDevice().getController());
    super.closedSession(deviceSessionManagement);
  }

  @Override
  protected String getSelector(int address){
    return selectorMap.containsKey(address) ? selectorMap.get(address) : super.getSelector(address);
  }

  private void processConfiguredDevices(){
    List<I2CDeviceConfigDTO> i2CDeviceConfigs = ((I2CBusConfig)properties).getDevices();
    for(I2CDeviceConfigDTO deviceConfig: i2CDeviceConfigs){
      configureDevice(deviceConfig);
    }
  }

  private void configureDevice(I2CDeviceConfigDTO deviceConfig){
    String name = deviceConfig.getName();
    int addr = deviceConfig.getAddress();
    String selector = deviceConfig.getSelector();
    if(name != null ){
      try {
        i2CBusManager.configureDevice(addr, name);
      } catch (IOException e) {
        // To Do
      }
      if(selector != null && !selector.isBlank()){
        selectorMap.put(addr, selector);
      }
    }
  }

  @Override
  protected Map<String, DeviceController> scan() {
    try {
      if(properties.isAutoScan()) {
        i2CBusManager.scanForDevices(10);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return i2CBusManager.getActive();
  }

}
