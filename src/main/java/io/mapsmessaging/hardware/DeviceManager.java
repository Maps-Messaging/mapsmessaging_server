/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.devices.DeviceBusManager;
import io.mapsmessaging.devices.i2c.I2CBusManager;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.i2c.I2CBusHandler;
import io.mapsmessaging.hardware.device.handler.onewire.OneWireBusHandler;
import io.mapsmessaging.hardware.trigger.PeriodicTrigger;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;

import java.io.IOException;
import java.util.*;

import static io.mapsmessaging.logging.ServerLogMessages.DEVICE_MANAGER_FAILED_TO_REGISTER;

public class DeviceManager implements ServiceManager, Agent {

  private final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
  private final List<ConfigurationProperties> devices;
  private final List<BusHandler> busHandlers;
  private final List<Trigger> triggers;
  private final Map<String, Trigger> configuredTriggers;
  private DeviceBusManager deviceBusManager;

  public DeviceManager() {
    logger.log(ServerLogMessages.DEVICE_MANAGER_STARTUP);
    devices = new ArrayList<>();
    busHandlers = new ArrayList<>();
    configuredTriggers = new LinkedHashMap<>();
    ServiceLoader<Trigger> triggerServices = ServiceLoader.load(Trigger.class);
    triggers = new ArrayList<>();
    for (Trigger trigger : triggerServices) {
      triggers.add(trigger);
    }
    ConfigurationProperties properties = ConfigurationManager.getInstance().getProperties("DeviceManager");

    DeviceBusManager manager = null;
    try {
      manager = DeviceBusManager.getInstance();
      if (!manager.isAvailable()) {
        manager = null;
      }
      manager = loadConfig(properties, manager);
    } catch (Throwable th) {
    }

    deviceBusManager = manager;
  }

  public boolean isEnabled() {
    return deviceBusManager != null;
  }

  private DeviceBusManager loadConfig(ConfigurationProperties properties, DeviceBusManager manager) {
    if (properties.getBooleanProperty("enabled", false) && manager != null) {
      Object obj = properties.get("data");
      if (obj instanceof List) {
        for(ConfigurationProperties props:(List<ConfigurationProperties>) obj){
          if(props.getProperty("name").equalsIgnoreCase("triggers")){
            loadTriggers(props);
          }
          else{
            devices.add(props);
          }
        }
      } else if (obj instanceof ConfigurationProperties) {
        devices.add((ConfigurationProperties) obj);
      }
      logger.log(ServerLogMessages.DEVICE_MANAGER_LOAD_PROPERTIES);
      try {
        for (ConfigurationProperties deviceConfig : devices) {
          if (deviceConfig.containsKey("name") &&
              deviceConfig.getProperty("name").equalsIgnoreCase("i2c") &&
              deviceConfig.getBooleanProperty("enabled", false)) {
            loadI2CConfig(manager, deviceConfig);
          }
          if (deviceConfig.containsKey("name") &&
              deviceConfig.getProperty("name").equalsIgnoreCase("oneWire") &&
              deviceConfig.getBooleanProperty("enabled", false)) {
            String triggerName = deviceConfig.getProperty("trigger", "default");
            Trigger trigger = locateNamedTrigger(triggerName);
            busHandlers.add(new OneWireBusHandler(manager.getOneWireBusManager(), deviceConfig, trigger));
          }
        }
        logger.log(ServerLogMessages.DEVICE_MANAGER_STARTUP_COMPLETE);
      } catch (Throwable th) {
        devices.clear();
        busHandlers.clear();
        manager = null;
        logger.log(ServerLogMessages.DEVICE_MANAGER_STARTUP_COMPLETE, th);
      }
    }
    return manager;
  }

  private void loadTriggers(ConfigurationProperties deviceConfig){
    Object configList = deviceConfig.get("config");
    if (configList instanceof List) {
      for (Object triggerConfigObj : (List) configList) {
        if (triggerConfigObj instanceof ConfigurationProperties) {
          configureTriggerConfig((ConfigurationProperties) triggerConfigObj);
        }
      }
    }
    if(!configuredTriggers.containsKey("default")){
      configuredTriggers.put("default", new PeriodicTrigger(60000));
    }
  }

  private void configureTriggerConfig(ConfigurationProperties triggerConfig){
    String type = triggerConfig.getProperty("type");
    for(Trigger trigger:triggers){
      if(trigger.getName().equalsIgnoreCase(type)){
        String name = triggerConfig.getProperty("name");
        try {
          Trigger actual = trigger.build(triggerConfig);
          configuredTriggers.put(name, actual);
        } catch (IOException e) {
          logger.log(DEVICE_MANAGER_FAILED_TO_REGISTER, name, e);
        }
      }
    }
  }

  private void loadI2CConfig(DeviceBusManager manager, ConfigurationProperties deviceConfig) {
    Object configList = deviceConfig.get("config");
    if (configList instanceof List) {
      for (Object busConfigObj : (List) configList) {
        if (busConfigObj instanceof ConfigurationProperties) {
          ((ConfigurationProperties) busConfigObj).setGlobal(deviceConfig.getGlobal());
          configureI2CConfig(manager, (ConfigurationProperties) busConfigObj);
        }
      }
    }
  }

  private void configureI2CConfig(DeviceBusManager manager, ConfigurationProperties busConfig) {
    for (int x = 0; x < manager.getI2cBusManager().length; x++) {
      if (busConfig.getIntProperty("bus", -1) == x && busConfig.getBooleanProperty("enabled", false)) {
        I2CBusManager busManager = manager.getI2cBusManager()[x];
        String triggerName = busConfig.getProperty("trigger", "default");
        Trigger trigger = locateNamedTrigger(triggerName);
        busHandlers.add(new I2CBusHandler(busManager, busConfig, trigger));
      }
    }
  }

  private Trigger locateNamedTrigger(String name){
    Trigger trigger = configuredTriggers.get(name);
    if(trigger == null){
      trigger = configuredTriggers.get("default");
    }
    return trigger;
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
    for(Trigger trigger:configuredTriggers.values()){
      trigger.start();
    }
  }

  public void stop() {
    stopAll();
    if (deviceBusManager != null) {
      deviceBusManager.close();
    }
  }

  public void initialise() {
    startAll();
  }

  public void startAll() {
    logger.log(ServerLogMessages.DEVICE_MANAGER_START_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.start();
    }
  }

  public void stopAll() {
    logger.log(ServerLogMessages.DEVICE_MANAGER_STOP_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.stop();
    }
  }

  public void pauseAll() {
    logger.log(ServerLogMessages.DEVICE_MANAGER_PAUSE_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.pause();
    }

  }

  public void resumeAll() {
    logger.log(ServerLogMessages.DEVICE_MANAGER_RESUME_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.resume();
    }
  }


  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    service.addAll(triggers);
    return service.listIterator();
  }
}