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

package io.mapsmessaging.hardware;

import io.mapsmessaging.devices.DeviceBusManager;
import io.mapsmessaging.devices.i2c.I2CBusManager;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.demo.DemoBusHandler;
import io.mapsmessaging.hardware.device.handler.i2c.I2CBusHandler;
import io.mapsmessaging.hardware.device.handler.onewire.OneWireBusHandler;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class DeviceManager implements ServiceManager, Agent {

  private final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
  private final List<ConfigurationProperties> devices;
  private final DeviceBusManager deviceBusManager;
  private final List<BusHandler> busHandlers;
  private final List<Trigger> triggers;

  public DeviceManager() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP);
    devices = new ArrayList<>();
    busHandlers = new ArrayList<>();
    ServiceLoader<Trigger> triggerServices = ServiceLoader.load(Trigger.class);
    triggers = new ArrayList<>();
    for (Trigger trigger : triggerServices) {
      triggers.add(trigger);
    }
    ConfigurationProperties properties = ConfigurationManager.getInstance().getProperties("DeviceManager");

    DeviceBusManager manager = null;
    try {
      manager = DeviceBusManager.getInstance();
    } catch (Throwable th) {
      // PI4J is not available, so we can disable this
    }

    deviceBusManager = manager;
    loadConfig(properties);
  }

  private void loadConfig(ConfigurationProperties properties){
    if( properties.getBooleanProperty("enabled", false) && deviceBusManager != null) {
      Object obj = properties.get("data");
      if (obj instanceof List) {
        devices.addAll((List<ConfigurationProperties>) obj);
      } else if (obj instanceof ConfigurationProperties) {
        devices.add((ConfigurationProperties) obj);
      }
      logger.log(ServerLogMessages.NETWORK_MANAGER_LOAD_PROPERTIES);

      for(ConfigurationProperties deviceConfig: devices) {
        if (deviceConfig.containsKey("name") &&
            deviceConfig.getProperty("name").equalsIgnoreCase("i2c") &&
            deviceConfig.getBooleanProperty("enabled", false)) {
          loadI2CConfig(deviceConfig);
        }

        if (deviceConfig.containsKey("name") &&
            deviceConfig.getProperty("name").equalsIgnoreCase("oneWire") &&
            deviceConfig.getBooleanProperty("enabled", false)) {
          busHandlers.add(new OneWireBusHandler(deviceBusManager.getOneWireBusManager(), deviceConfig));
        }

        if (deviceConfig.containsKey("name") &&
            deviceConfig.getProperty("name").equalsIgnoreCase("demo") &&
            deviceConfig.getBooleanProperty("enabled", false)) {
          busHandlers.add(new DemoBusHandler(deviceConfig));
        }
      }
      logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP_COMPLETE);
    }
  }

  private void loadI2CConfig(ConfigurationProperties deviceConfig){
    Object configList = deviceConfig.get("config");
    if (configList instanceof List) {
      for (Object busConfigObj : (List) configList) {
        if (busConfigObj instanceof ConfigurationProperties) {
          configureI2CConfig((ConfigurationProperties) busConfigObj);
        }
      }
    }
  }

  private void configureI2CConfig(ConfigurationProperties busConfig){
    for (int x = 0; x < deviceBusManager.getI2cBusManager().length; x++) {
      if (busConfig.getIntProperty("bus", -1) == x && busConfig.getBooleanProperty("enabled", false)) {
        I2CBusManager busManager = deviceBusManager.getI2cBusManager()[x];
        busHandlers.add(new I2CBusHandler(busManager, busConfig));
      }
    }
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
    deviceBusManager.close();
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