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

package io.mapsmessaging.hardware.device.handler.serial;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.config.device.SerialDeviceBusConfig;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.serial.SerialBusManager;
import io.mapsmessaging.devices.serial.SerialDeviceController;
import io.mapsmessaging.dto.rest.config.device.SerialDeviceDTO;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortScanner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SerialDeviceBusHandler extends BusHandler {

  private final SerialBusManager serialBusManager;

  public SerialDeviceBusHandler(SerialBusManager serialBusManager, SerialDeviceBusConfig serialDeviceDTO, Trigger trigger) {
    super(serialDeviceDTO, trigger);
    this.serialBusManager = serialBusManager;
  }

  @Override
  protected Map<String, DeviceController> scan() {
    Map<String, DeviceController> map = new HashMap<>();
    List<SerialDeviceDTO> list = ((SerialDeviceBusConfig)properties ).getDevices();
    for(SerialDeviceDTO serialDeviceConfig: list){
      String portName = serialDeviceConfig.getSerialConfig().getPort();
      if(!foundDevices.containsKey(portName)){
        SerialPort port = SerialPortScanner.getInstance().allocatePort(portName);
        if(port != null){
          try {
            SerialEndPoint.configure(port, serialDeviceConfig.getSerialConfig());
            SerialDeviceController controller = serialBusManager.mount(serialDeviceConfig.getName(), new Serial(port));
            map.put(portName, controller);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return map;
  }

  @Override
  protected DeviceHandler createDeviceHander(String key, DeviceController controller) {
    return new SerialDeviceHandler(key, controller);
  }

}