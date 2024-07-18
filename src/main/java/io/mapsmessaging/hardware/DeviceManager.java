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

import static io.mapsmessaging.logging.ServerLogMessages.DEVICE_MANAGER_FAILED_TO_REGISTER;

import io.mapsmessaging.config.DeviceManagerConfig;
import io.mapsmessaging.config.device.I2CBusConfig;
import io.mapsmessaging.config.device.OneWireBusConfig;
import io.mapsmessaging.config.device.triggers.TriggerConfig;
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
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import java.io.IOException;
import java.util.*;

public class DeviceManager implements ServiceManager, Agent {

  private static final String DEFAULT = "default";

  private final boolean enabled;
  private final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
  private final List<BusHandler> busHandlers;
  private final List<Trigger> triggers;
  private final Map<String, Trigger> configuredTriggers;
  private final DeviceBusManager deviceBusManager;

  public DeviceManager() {
    triggers = new ArrayList<>();
    configuredTriggers = new LinkedHashMap<>();
    busHandlers = new ArrayList<>();
    DeviceManagerConfig deviceManagerConfig = DeviceManagerConfig.getInstance();
    enabled = deviceManagerConfig.isEnabled();
    if(!enabled){
      deviceBusManager = null;
      return;
    }
    logger.log(ServerLogMessages.DEVICE_MANAGER_STARTUP);
    ServiceLoader<Trigger> triggerServices = ServiceLoader.load(Trigger.class);
    for (Trigger trigger : triggerServices) {
      triggers.add(trigger);
    }
    DeviceBusManager manager = null;
    try {
      manager = DeviceBusManager.getInstance();
      if (manager.isAvailable()) {
        loadConfig(deviceManagerConfig, manager);
      }
      else{
        manager = null;
      }
    } catch (Throwable th) {
      th.printStackTrace();
    }

    deviceBusManager = manager;
  }

  public boolean isEnabled() {
    return deviceBusManager != null;
  }

  private void loadConfig(DeviceManagerConfig deviceManagerConfig, DeviceBusManager manager) {
    if(deviceManagerConfig.isEnabled()){
      loadTriggers(deviceManagerConfig.getTriggers());
      logger.log(ServerLogMessages.DEVICE_MANAGER_LOAD_PROPERTIES);
      loadI2CConfig(manager, deviceManagerConfig.getI2cBuses());
      if(deviceManagerConfig.getOneWireBus() != null){
        OneWireBusConfig oneWireBusConfig = deviceManagerConfig.getOneWireBus();
        Trigger trigger = locateNamedTrigger(oneWireBusConfig.getTrigger());
        busHandlers.add(new OneWireBusHandler(manager.getOneWireBusManager(), oneWireBusConfig, trigger));
      }
    }
  }

  private void loadTriggers(List<TriggerConfig> triggerConfigList){
    if(triggerConfigList == null) return;
    for(TriggerConfig triggerConfig:triggerConfigList){
      String type = triggerConfig.getType();
      for(Trigger trigger:triggers){
        if(trigger.getName().equalsIgnoreCase(type)){
          String name = triggerConfig.getName();
          try {
            Trigger actual = trigger.build(triggerConfig);
            configuredTriggers.put(name, actual);
          } catch (IOException e) {
            logger.log(DEVICE_MANAGER_FAILED_TO_REGISTER, name, e);
          }
        }
      }
    }
    if(!configuredTriggers.containsKey(DEFAULT)){
      configuredTriggers.put(DEFAULT, new PeriodicTrigger(60000));
    }
  }

  private void loadI2CConfig(DeviceBusManager manager, List<I2CBusConfig> deviceBusConfigs) {
    for(I2CBusConfig i2CDeviceConfig:deviceBusConfigs){
      configureI2CConfig(manager,i2CDeviceConfig);
    }
  }

  private void configureI2CConfig(DeviceBusManager manager, I2CBusConfig busConfig) {
    for (int x = 0; x < manager.getI2cBusManager().length; x++) {
      if (busConfig.getBus() == x && busConfig.isEnabled()) {
        I2CBusManager busManager = manager.getI2cBusManager()[x];
        String triggerName = busConfig.getTrigger();
        Trigger trigger = locateNamedTrigger(triggerName);
        busHandlers.add(new I2CBusHandler(busManager, busConfig, trigger));
      }
    }
  }

  private Trigger locateNamedTrigger(String name){
    Trigger trigger = configuredTriggers.get(name);
    if(trigger == null){
      trigger = configuredTriggers.get(DEFAULT);
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
    if(!enabled) return;
    initialise();
    for(Trigger trigger:configuredTriggers.values()){
      trigger.start();
    }
  }

  public void stop() {
    if(!enabled) return;
    stopAll();
    if (deviceBusManager != null) {
      deviceBusManager.close();
    }
  }

  public void initialise() {
    if(!enabled) return;
    startAll();
  }

  public void startAll() {
    if(!enabled) return;
    logger.log(ServerLogMessages.DEVICE_MANAGER_START_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.start();
    }
  }

  public void stopAll() {
    if(!enabled) return;
    logger.log(ServerLogMessages.DEVICE_MANAGER_STOP_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.stop();
    }
  }

  public void pauseAll() {
    if(!enabled) return;
    logger.log(ServerLogMessages.DEVICE_MANAGER_PAUSE_ALL);
    for(BusHandler busHandler:busHandlers){
      busHandler.pause();
    }

  }

  public void resumeAll() {
    if(!enabled) return;
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