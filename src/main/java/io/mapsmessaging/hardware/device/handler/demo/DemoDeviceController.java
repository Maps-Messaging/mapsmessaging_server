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

package io.mapsmessaging.hardware.device.handler.demo;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.DeviceType;
import io.mapsmessaging.schemas.config.SchemaConfig;

import java.io.IOException;
import java.time.LocalDateTime;

public class DemoDeviceController implements DeviceController {
  @Override
  public String getName() {
    return "demo";
  }

  @Override
  public String getDescription() {
    return "Demo Device";
  }

  @Override
  public SchemaConfig getSchema() {
    return null;
  }

  @Override
  public byte[] getDeviceConfiguration() throws IOException {
    return new byte[0];
  }

  @Override
  public DeviceType getType() {
    return DeviceType.SENSOR;
  }

  @Override
  public byte[] getDeviceState() {
    return ("{ \"time\": \""+ LocalDateTime.now().toString()+" \"}").getBytes();
  }

}
