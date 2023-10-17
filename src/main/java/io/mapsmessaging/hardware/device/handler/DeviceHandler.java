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

package io.mapsmessaging.hardware.device.handler;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.DeviceType;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.schemas.config.SchemaConfig;
import lombok.Data;

import java.io.IOException;

@Data
public abstract class DeviceHandler {

  private final DeviceController controller;
  private Trigger trigger;

  protected DeviceHandler(DeviceController controller){
    this.controller = controller;
  }

  public abstract String getBusName();

  public String getName(){
    return controller.getName();
  }

  public String getVersion(){
    return "1.0";
  }

  public byte[] getData() throws IOException {
    return controller.getDeviceState();
  }

  public byte[] getConfiguration() throws IOException {
    return controller.getDeviceConfiguration();
  }

  public byte[] updateConfig(byte[] config) throws IOException{
    return controller.updateDeviceConfiguration(config);
  }

  public SchemaConfig getSchema(){
    return controller.getSchema();
  }

  public DeviceType getType(){
    return controller.getType();
  }

  public boolean enableConfig(){
    return false;
  }

  public boolean enableRaw(){
    return false;
  }

}
