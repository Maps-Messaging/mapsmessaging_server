/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import io.mapsmessaging.logging.ServerLogMessages;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.config.device.SerialDeviceBusConfig;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.serial.SerialBusManager;
import io.mapsmessaging.devices.serial.SerialDeviceController;
import io.mapsmessaging.devices.serial.ComputedSerialDeviceController;
import io.mapsmessaging.hardware.device.DeviceSessionManagement;
import io.mapsmessaging.dto.rest.config.device.SerialBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SerialBusDeviceDTO;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortScanner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SerialDeviceBusHandler extends BusHandler {

  private final SerialBusManager serialBusManager;
  private final Map<String, DeviceSessionManagement> activeSessions = new ConcurrentHashMap<>();
  private final Map<String, String> canonicalPorts = new ConcurrentHashMap<>();

  public SerialDeviceBusHandler(SerialBusManager serialBusManager, SerialBusConfigDTO serialDeviceDTO, Trigger trigger) {
    super(serialDeviceDTO, trigger);
    this.serialBusManager = serialBusManager;
  }

  @Override
  protected synchronized Map<String, DeviceController> scan() {
    Map<String, DeviceController> map = new HashMap<>();
    List<SerialBusDeviceDTO> list = ((SerialDeviceBusConfig) properties).getDevices();

    for (SerialBusDeviceDTO serialDeviceConfig : list) {
      String configuredPort = serialDeviceConfig.getSerialConfig().getPort();
      SerialPort port = SerialPortScanner.getInstance().allocatePort(configuredPort);
      if (port == null) {
        String previousPort = canonicalPorts.get(configuredPort);
        if (previousPort != null) {
          disconnectPort(previousPort);
        }
        continue;
      }

      // The library indexes the actual system port name, which may differ from an alias.
      String portName = port.getSystemPortName();
      canonicalPorts.put(configuredPort, portName);
      if (!foundDevices.containsKey(portName) && !serialBusManager.getActive().containsKey(portName)) {
        try {
          SerialEndPoint.configure(port, serialDeviceConfig.getSerialConfig());
          serialBusManager.mount(serialDeviceConfig.getName(), new Serial(port));
        } catch (IOException exception) {
          logger.log(ServerLogMessages.DEVICE_MANAGER_MOUNT_FAILED, portName, exception);
        }
      }

      for (Map.Entry<String, DeviceController> entry : serialBusManager.getActive().entrySet()) {
        if (entry.getKey().equals(portName) || entry.getKey().startsWith(portName + "_")) {
          map.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return map;
  }

  private void disconnectPort(String portName) {
    DeviceSessionManagement primarySession = activeSessions.get(portName);
    if (primarySession != null) {
      try {
        primarySession.stop();
      } catch (IOException exception) {
        logger.log(ServerLogMessages.DEVICE_PUBLISH_EXCEPTION, exception);
      } finally {
        closedSession(primarySession);
      }
      return;
    }

    DeviceController controller = serialBusManager.getActive().get(portName);
    if (controller instanceof SerialDeviceController primary) {
      cleanupPhysicalDevice(portName, primary);
    }
  }

  private void cleanupPhysicalDevice(String portName, SerialDeviceController primary) {
    // Stop every computed session before closing the shared hardware.
    for (Map.Entry<String, DeviceSessionManagement> entry : new HashMap<>(activeSessions).entrySet()) {
      if (entry.getKey().startsWith(portName + "_")
          && activeSessions.remove(entry.getKey(), entry.getValue())) {
        try {
          entry.getValue().stop();
        } catch (IOException exception) {
          logger.log(ServerLogMessages.DEVICE_PUBLISH_EXCEPTION, exception);
        } finally {
          foundDevices.remove(entry.getKey(), entry.getValue().getDevice());
        }
      }
    }
    for (String key : new HashMap<>(foundDevices).keySet()) {
      if (key.startsWith(portName + "_")) {
        foundDevices.remove(key);
      }
    }

    foundDevices.remove(portName);
    serialBusManager.unmount(primary);
    primary.close();
    canonicalPorts.entrySet().removeIf(entry -> entry.getValue().equals(portName));
  }

  @Override
  protected String getSessionName(DeviceHandler deviceHandler) {
    return deviceHandler.getBusName() + "_" + deviceHandler.getKey();
  }

  @Override
  protected void deviceSessionCreated(DeviceSessionManagement session) {
    activeSessions.put(session.getDevice().getKey(), session);
  }

  @Override
  protected void deviceSessionCreationFailed(DeviceSessionManagement session) {
    closedSession(session);
  }

  @Override
  public synchronized void closedSession(DeviceSessionManagement session) {
    DeviceHandler device = session.getDevice();
    String key = device.getKey();
    if (!activeSessions.remove(key, session)) {
      // A delayed failure must never remove the replacement session.
      return;
    }
    foundDevices.remove(key, device);

    if (device.getController() instanceof SerialDeviceController primary
        && !(primary instanceof ComputedSerialDeviceController)) {
      cleanupPhysicalDevice(key, primary);
    }
  }

  @Override
  protected DeviceHandler createDeviceHander(String key, DeviceController controller) {
    return new SerialDeviceHandler(key, controller);
  }

}