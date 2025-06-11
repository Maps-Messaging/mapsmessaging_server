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

package io.mapsmessaging.hardware.device.handler;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.DeviceType;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.schemas.config.SchemaConfig;
import lombok.Data;

import java.io.IOException;
import java.util.UUID;

@Data
public abstract class DeviceHandler {

  private final DeviceController controller;
  private final String key;
  private Trigger trigger;


  protected DeviceHandler(String key, DeviceController controller){
    this.key = key;
    this.controller = controller;
  }

  public abstract String getBusName();

  public int getBusNumber(){
    return -1;
  }

  public int getDeviceAddress(){
    return -1;
  }

  public String getTopicName(String template){
    while(template.contains("[bus_name]")) {
      template = template.replace("[bus_name]", getBusName());
    }

    String busNumber = getBusNumber() >= 0? ""+getBusNumber(): "";
    while(template.contains("[bus_number]")) {
      template = template.replace("[bus_number]", busNumber);
    }

    String deviceAddress = getDeviceAddress() >= 0? "0x"+Integer.toHexString(getDeviceAddress()): "";
    while(template.contains("[device_addr]")) {
      template = template.replace("[device_addr]", deviceAddress);
    }

    while(template.contains("[device_name]")) {
      template = template.replace("[device_name]", getName());
    }
    while(template.contains("[device_type]")) {
      template = template.replace("[device_type]", controller.getType().name().toLowerCase());
    }
    while(template.contains("//")){
      template = template.replace("//", "/");
    }
    return template;
  }

  public UUID getSchemaId() {
    return controller.getSchemaId();
  }

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
