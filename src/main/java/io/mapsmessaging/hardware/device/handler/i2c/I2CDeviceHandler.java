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

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.i2c.I2CDeviceController;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;

public class I2CDeviceHandler extends DeviceHandler {

  public I2CDeviceHandler(String key, DeviceController deviceController){
    super(key, deviceController);
  }

  @Override
  public String getBusName() {
    return "i2c";
  }

  @Override
  public int getBusNumber(){
    if(getController() instanceof I2CDeviceController){
      return ((I2CDeviceController)getController()).getDevice().getBus();
    }
    return super.getBusNumber();
  }

  @Override
  public int getDeviceAddress(){
    if(getController() instanceof I2CDeviceController){
      return ((I2CDeviceController)getController()).getDevice().getDevice().getDevice();
    }
    return super.getDeviceAddress();
  }
  @Override
  public boolean enableConfig(){
    return true;
  }

  @Override
  public boolean enableRaw(){
    return true;
  }
}
