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

package io.mapsmessaging.hardware.trigger;

import com.pi4j.io.gpio.digital.DigitalState;
import io.mapsmessaging.devices.DeviceBusManager;
import io.mapsmessaging.devices.gpio.InterruptFactory;
import io.mapsmessaging.devices.gpio.InterruptListener;
import io.mapsmessaging.devices.gpio.InterruptPin;
import io.mapsmessaging.dto.rest.config.device.triggers.BaseTriggerConfigDTO;
import io.mapsmessaging.dto.rest.config.device.triggers.InterruptTriggerConfigDTO;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class InterruptTrigger extends Trigger implements InterruptListener {

  private final InterruptPin interruptPin;

  public InterruptTrigger(){
    interruptPin = null;
  }

  public InterruptTrigger(InterruptPin pin){
    interruptPin = pin;
  }

  @Override
  public Trigger build(BaseTriggerConfigDTO properties) throws IOException {
    InterruptTriggerConfigDTO triggerConfig = (InterruptTriggerConfigDTO)properties;
    InterruptFactory interruptFactory = DeviceBusManager.getInstance().getInterruptFactory();
    Map<String, String> factoryMap = new LinkedHashMap<>();
    factoryMap.put("id", triggerConfig.getId());
    factoryMap.put("address", ""+triggerConfig.getAddress());
    factoryMap.put("name", triggerConfig.getName());
    factoryMap.put("pull", triggerConfig.getPullDirection());
    return new InterruptTrigger(interruptFactory.allocateInterruptPin(factoryMap));
  }

  @Override
  public void start() {
    if(interruptPin != null) {
      interruptPin.addListener(this);
    }
  }

  @Override
  public void stop() {
    if(interruptPin != null) {
      interruptPin.removeListener(this);
    }
  }

  @Override
  public String getName() {
    return "interrupt";
  }

  @Override
  public String getDescription() {
    return "Triggers device read on interrupt";
  }

  @Override
  public void interrupt(InterruptPin pin, DigitalState state) {
    runActions();
  }
}
