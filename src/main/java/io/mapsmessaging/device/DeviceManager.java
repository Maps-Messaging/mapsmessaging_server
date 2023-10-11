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

package io.mapsmessaging.device;

import io.mapsmessaging.device.handler.BusHandler;
import io.mapsmessaging.device.handler.i2c.I2CBusHandler;
import io.mapsmessaging.device.handler.onewire.OneWireBusHandler;
import io.mapsmessaging.devices.DeviceBusManager;
import io.mapsmessaging.devices.i2c.I2CBusManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DeviceManager implements ServiceManager, Agent {

  private final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
  private final List<ConfigurationProperties> devices;
  private final DeviceBusManager deviceBusManager;
  private final List<BusHandler> busHandlers;

  public DeviceManager() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP);
    deviceBusManager = DeviceBusManager.getInstance();
    busHandlers = new ArrayList<>();
    ConfigurationProperties properties = ConfigurationManager.getInstance().getProperties("DeviceManager");
    Object obj = properties.get("data");
    devices = new ArrayList<>();
    if (obj instanceof List) {
      devices.addAll((List<ConfigurationProperties>) obj);
    } else if (obj instanceof ConfigurationProperties) {
      devices.add((ConfigurationProperties) obj);
    }
    logger.log(ServerLogMessages.NETWORK_MANAGER_LOAD_PROPERTIES);

    for(I2CBusManager busManager:deviceBusManager.getI2cBusManager()){
      busHandlers.add(new I2CBusHandler(busManager));
    }
    busHandlers.add(new OneWireBusHandler(deviceBusManager.getOneWireBusManager()));

    logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP_COMPLETE);
  }

  @Override
  public String getName() {
    return "Device Manager";
  }

  @Override
  public String getDescription() {
    return "Manages all of the physical devices available to the server";
  }

  public void start() {
    initialise();
  }

  public void stop() {
    stopAll();
  }

  public void initialise() {
    for (ConfigurationProperties configurationProperties : devices) {

    }
    startAll();
  }

  public void startAll() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_START_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.start();
    }
  }

  public void stopAll() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_STOP_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.stop();
    }
  }

  public void pauseAll() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_PAUSE_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.pause();
    }

  }

  public void resumeAll() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_RESUME_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.resume();
    }
  }


  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    return service.listIterator();
  }
}