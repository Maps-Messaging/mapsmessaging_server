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

package io.mapsmessaging.hardware.device.handler.onewire;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.onewire.OneWireBusManager;
import io.mapsmessaging.dto.rest.config.device.OneWireBusConfigDTO;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;

import java.util.Map;

public class OneWireBusHandler extends BusHandler {

  private final OneWireBusManager oneWireBusManager;

  public OneWireBusHandler(OneWireBusManager oneWireBusManager, OneWireBusConfigDTO oneWireBusConfig, Trigger trigger) {
    super(oneWireBusConfig, trigger);
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