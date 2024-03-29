/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.hardware.device.handler.onewire;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.onewire.OneWireBusManager;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;

import java.util.Map;

public class OneWireBusHandler extends BusHandler {

  private final OneWireBusManager oneWireBusManager;

  public OneWireBusHandler(OneWireBusManager oneWireBusManager, ConfigurationProperties properties, Trigger trigger) {
    super(properties, trigger);
    this.oneWireBusManager = oneWireBusManager;
  }

  @Override
  protected Map<String, DeviceController> scan() {
    oneWireBusManager.scan();
    return oneWireBusManager.getActive();
  }

  @Override
  protected DeviceHandler createDeviceHander(String key, DeviceController controller) {
    return new OneWireDeviceHandler(key, controller);
  }


}