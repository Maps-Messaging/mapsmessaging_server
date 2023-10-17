/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.hardware.device.handler.i2c;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.i2c.I2CBusManager;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;

import java.util.Map;

public class I2CBusHandler extends BusHandler {

  private final I2CBusManager i2CBusManager;

  public I2CBusHandler(I2CBusManager i2CBusManager, ConfigurationProperties properties, Trigger trigger){
    super(properties, trigger);
    this.i2CBusManager = i2CBusManager;
  }

  @Override
  protected DeviceHandler createDeviceHander(DeviceController controller) {
    return new I2CDeviceHandler(controller);
  }

  @Override
  protected Map<String, DeviceController> scan() {
    try {
      i2CBusManager.scanForDevices(10);
    } catch (InterruptedException e) {
      //
    }
    return i2CBusManager.getActive();
  }


  public boolean enableConfig(){
    return true;
  }

  public boolean enableRaw(){
    return true;
  }
}
