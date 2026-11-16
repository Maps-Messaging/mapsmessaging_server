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

package io.mapsmessaging.hardware;

import io.mapsmessaging.config.DeviceManagerConfig;
import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.hardware.device.DeviceSessionManagement;
import org.jetbrains.annotations.NotNull;
import lombok.NonNull;
import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import io.mapsmessaging.devices.DeviceBusManager;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.devices.i2c.I2CBusManager;
import io.mapsmessaging.dto.rest.config.device.I2CBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.OneWireBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SpiDeviceBusConfigDTO;
import io.mapsmessaging.dto.rest.config.device.triggers.BaseTriggerConfigDTO;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.i2c.I2CBusHandler;
import io.mapsmessaging.hardware.device.handler.onewire.OneWireBusHandler;
import io.mapsmessaging.hardware.device.handler.serial.SerialDeviceBusHandler;
import io.mapsmessaging.hardware.device.handler.spi.SpiBusHandler;
import io.mapsmessaging.hardware.trigger.PeriodicTrigger;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;

import java.io.IOException;
import java.util.*;

import static io.mapsmessaging.logging.ServerLogMessages.DEVICE_MANAGER_FAILED_TO_REGISTER;

public class DeviceManager implements ServiceManager, Agent, ClientConnection, MessageListener {

  private static final String DEFAULT = "default";
  private final boolean enabled;
  private final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
  private final List<BusHandler> busHandlers;
  private final List<Trigger> triggers;
  private final Map<String, Trigger> configuredTriggers;
  private final boolean enableI2C;
  private final boolean enableOneWire;
  private final boolean enableSpi;
  private final boolean enableSerial;


  private final DeviceBusManager deviceBusManager;
  private String errorMessage;
  private final Map<String, DeviceSessionManagement> subscribers = new ConcurrentHashMap<>();
  private volatile Session session;
  private boolean starting;

  public DeviceManager(FeatureManager featureManager) {
    enableSpi = featureManager.isEnabled("hardware.spi");
    enableI2C = featureManager.isEnabled("hardware.i2c");
    enableOneWire = featureManager.isEnabled("hardware.oneWire");
    enableSerial = true;

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
      if (manager.isAvailable() ||  deviceManagerConfig.isDemoEnabled() || deviceManagerConfig.getSerialDeviceBusConfig() != null) {
        loadConfig(deviceManagerConfig, manager);
      }
      else{
        manager = null;
      }
    } catch (Throwable th) {
      manager = null;
      errorMessage = th.getMessage();
      logger.log(ServerLogMessages.DEVICE_MANAGER_STARTUP_FAILED, th);
    }

    deviceBusManager = manager;
  }

  public  List<String> scan() throws InterruptedException {
    List<String> scanResults = new ArrayList<>();
    if(deviceBusManager == null) return scanResults;
    for(I2CBusManager i2CBusManager:deviceBusManager.getI2cBusManager()){
      List<Integer> list = i2CBusManager.findDevicesOnBus(0);
      scanResults.addAll(i2CBusManager.listDetected(list));
    }
    return scanResults;
  }

  public List<DeviceController> getActiveDevices(){
    List<DeviceController> devices = new ArrayList<>();
    if(enableI2C) {
      devices.addAll(deviceBusManager.getI2cBusManager()[0].getActive().values());
      devices.addAll(deviceBusManager.getI2cBusManager()[1].getActive().values());
      if(deviceBusManager.getI2cBusManager().length>2) {
        devices.addAll(deviceBusManager.getI2cBusManager()[2].getActive().values());
      }
    }
    if(enableOneWire) {
      devices.addAll(deviceBusManager.getOneWireBusManager().getActive().values());
    }
    if(enableSpi) {
      devices.addAll(deviceBusManager.getSpiBusManager().getActive().values());
    }
    if(enableSerial){
      devices.addAll(deviceBusManager.getSerialBusManager().getActive().values());
    }
    return devices;
  }

  public boolean isEnabled() {
    return deviceBusManager != null;
  }

  private void loadConfig(DeviceManagerConfig deviceManagerConfig, DeviceBusManager manager) {
    if(deviceManagerConfig.isEnabled()){
      loadTriggers(deviceManagerConfig.getTriggers());
      logger.log(ServerLogMessages.DEVICE_MANAGER_LOAD_PROPERTIES);
      if(enableI2C) {
        loadI2CConfig(manager, deviceManagerConfig.getI2cBuses());
      }
      if(deviceManagerConfig.getSpiBus() != null && enableSpi){
        SpiDeviceBusConfigDTO spiBusConfig = deviceManagerConfig.getSpiBus();
        if(spiBusConfig.isEnabled()){
          Trigger trigger = locateNamedTrigger(spiBusConfig.getTrigger());
          busHandlers.add(new SpiBusHandler( manager.getSpiBusManager(), spiBusConfig, trigger));
        }
      }
      if(deviceManagerConfig.getOneWireBus() != null && enableOneWire) {
        OneWireBusConfigDTO oneWireBusConfig = deviceManagerConfig.getOneWireBus();
        Trigger trigger = locateNamedTrigger(oneWireBusConfig.getTrigger());
        busHandlers.add(new OneWireBusHandler(manager.getOneWireBusManager(), oneWireBusConfig, trigger));
      }
      if(deviceManagerConfig.getSerialDeviceBusConfig() != null && enableSerial) {
        Trigger trigger = locateNamedTrigger(deviceManagerConfig.getSerialDeviceBusConfig().getTrigger());
        SerialDeviceBusHandler serialDeviceBusHandler = new SerialDeviceBusHandler(manager.getSerialBusManager(), deviceManagerConfig.getSerialDeviceBusConfig(), trigger);
        busHandlers.add(serialDeviceBusHandler);
      }
    }
  }

  private void loadTriggers(List<BaseTriggerConfigDTO> triggerConfigList){
    if(triggerConfigList == null) return;
    for(BaseTriggerConfigDTO triggerConfig:triggerConfigList){
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
    configuredTriggers.computeIfAbsent(DEFAULT, k -> new PeriodicTrigger(60000));
  }

  private void loadI2CConfig(DeviceBusManager manager, List<I2CBusConfigDTO> deviceBusConfigs) {
    for(I2CBusConfigDTO i2CDeviceConfig:deviceBusConfigs){
      configureI2CConfig(manager,i2CDeviceConfig);
    }
  }

  private void configureI2CConfig(DeviceBusManager manager, I2CBusConfigDTO busConfig) {
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

  public synchronized void start() {
    if (!enabled || deviceBusManager == null || starting) return;
    starting = true;
    SessionContextBuilder builder = new SessionContextBuilder("hardware", this);
    builder.setPersistentSession(false)
        .setResetState(true)
        .setUsername("anonymous")
        .setPassword(new char[0])
        .isInternal(true)
        .setReceiveMaximum(10);
    SessionManager.getInstance().createAsync(builder.build(), this).whenComplete((created, failure) -> {
      if (failure != null) {
        synchronized (DeviceManager.this) {
          starting = false;
          errorMessage = failure.getMessage();
        }
        logger.log(ServerLogMessages.DEVICE_MANAGER_STARTUP_FAILED, failure);
        return;
      }
      try {
        created.login();
        synchronized (DeviceManager.this) {
          if (!starting) {
            SessionManager.getInstance().closeAsync(created, true);
            return;
          }
          session = created;
          for (BusHandler busHandler : busHandlers) {
            busHandler.setHardwareManager(this);
          }
          startAll();
          for (Trigger trigger : configuredTriggers.values()) {
            trigger.start();
          }
        }
      } catch (Exception exception) {
        synchronized (DeviceManager.this) {
          starting = false;
          errorMessage = exception.getMessage();
        }
        logger.log(ServerLogMessages.DEVICE_MANAGER_STARTUP_FAILED, exception);
        SessionManager.getInstance().closeAsync(created, true);
      }
    });
  }

  public synchronized void stop() {
    if (!enabled) return;
    starting = false;
    stopAll();
    if (deviceBusManager != null) {
      deviceBusManager.close();
    }
    Session current = session;
    session = null;
    if (current != null) {
      try {
        SessionManager.getInstance().close(current, true);
      } catch (IOException exception) {
        logger.log(ServerLogMessages.DEVICE_PUBLISH_EXCEPTION, exception);
      }
    }
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    if(enabled){
      if(deviceBusManager == null){
        status.setStatus(Status.ERROR);
        status.setComment(errorMessage);
      }
      else{
        status.setStatus(Status.OK);
      }
    }
    else{
      status.setStatus(Status.DISABLED);
    }
    return status;
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

  public Session getSession() {
    return session;
  }

  public SubscribedEventManager subscribe(Destination destination, DeviceSessionManagement device) throws IOException {
    String name = destination.getFullyQualifiedNamespace();
    if (subscribers.putIfAbsent(name, device) != null) {
      throw new IOException("Hardware destination already subscribed: " + name);
    }
    try {
      SubscriptionContextBuilder builder = new SubscriptionContextBuilder(name, ClientAcknowledgement.AUTO);
      builder.setQos(QualityOfService.AT_MOST_ONCE);
      builder.setNoLocalMessages(true);
      return session.addSubscription(builder.build());
    } catch (IOException | RuntimeException exception) {
      subscribers.remove(name, device);
      throw exception;
    }
  }

  public void unsubscribe(Destination destination, DeviceSessionManagement device) {
    if (destination != null && subscribers.remove(destination.getFullyQualifiedNamespace(), device)) {
      session.removeSubscription(destination.getFullyQualifiedNamespace());
    }
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent event) {
    DeviceSessionManagement target = subscribers.get(event.getDestinationName());
    if (target != null) {
      target.sendMessage(event);
    } else {
      event.getCompletionTask().run();
    }
  }

  @Override
  public long getTimeOut() {
    return 0;
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void sendKeepAlive() {
  }

  @Override
  public Principal getPrincipal() {
    return () -> "hardware";
  }

  @Override
  public String getAuthenticationConfig() {
    return "";
  }

  @Override
  public String getUniqueName() {
    return "hardware";
  }

  @Override
  public String getProtocolName() {
    return "";
  }

  @Override
  public String getRemoteIp() {
    return "";
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>(triggers);
    return service.listIterator();
  }
}