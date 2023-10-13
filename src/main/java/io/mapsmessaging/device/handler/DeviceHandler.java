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

package io.mapsmessaging.device.handler;

import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

public abstract class DeviceHandler implements MessageListener {

  private final DeviceController controller;
  @Getter
  @Setter
  private Session session;

  @Getter
  @Setter
  private ProtocolMessageTransformation transformation;

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

}
