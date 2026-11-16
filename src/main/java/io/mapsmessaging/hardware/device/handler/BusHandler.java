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

package io.mapsmessaging.hardware.device.handler;

import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.dto.rest.config.device.DeviceBusConfigDTO;
import io.mapsmessaging.hardware.DeviceManager;
import io.mapsmessaging.hardware.device.DeviceSessionManagement;
import io.mapsmessaging.hardware.device.filter.DataFilter;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

import static io.mapsmessaging.logging.ServerLogMessages.DEVICE_MANAGER_MOUNT_FAILED;

public abstract class BusHandler implements Runnable {

  protected final Logger logger = LoggerFactory.getLogger(BusHandler.class);
  protected final Map<String, DeviceHandler> foundDevices;
  protected final DeviceBusConfigDTO properties;
  private final int scanPeriod;
  private Future<?> scheduledFuture;
  private final Trigger trigger;
  private final String topicNameTemplate;
  protected final Map<String, DeviceSessionManagement> activeSessions = new ConcurrentHashMap<>();
  private DeviceManager hardwareManager;

  public void setHardwareManager(DeviceManager hardwareManager) {
    this.hardwareManager = hardwareManager;
  }


  protected BusHandler(DeviceBusConfigDTO properties, Trigger trigger){
    foundDevices = new ConcurrentHashMap<>();
    this.properties = properties;
    this.trigger = trigger;
    scanPeriod = properties.getScanTime();
    topicNameTemplate = properties.getTopicNameTemplate();
  }

  public synchronized void start() {
    scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, 5000, scanPeriod, TimeUnit.MILLISECONDS);
  }

  public synchronized void stop(){
    if(scheduledFuture != null){
      scheduledFuture.cancel(true);
    }
    java.util.List<DeviceSessionManagement> sessions = new java.util.ArrayList<>(activeSessions.values());
    for (DeviceSessionManagement device : sessions) {
      try {
        device.stop();
      } catch (IOException | RuntimeException exception) {
        logger.log(io.mapsmessaging.logging.ServerLogMessages.DEVICE_PUBLISH_EXCEPTION, exception);
      }
    }
    // Computed devices release their state before the physical owner.
    sessions.sort(java.util.Comparator.comparingInt((DeviceSessionManagement device) -> device.getDevice().getKey().length()).reversed());
    for (DeviceSessionManagement device : sessions) {
      if (activeSessions.containsKey(device.getDevice().getKey())) {
        try {
          closedSession(device);
        } catch (RuntimeException exception) {
          logger.log(io.mapsmessaging.logging.ServerLogMessages.DEVICE_PUBLISH_EXCEPTION, exception);
        }
      }
    }
  }

  public void pause() {
  }

  public void resume() {
  }

  public void closedSession(DeviceSessionManagement deviceSessionManagement){
    activeSessions.remove(deviceSessionManagement.getDevice().getKey(), deviceSessionManagement);
    foundDevices.remove(deviceSessionManagement.getDevice().getKey(), deviceSessionManagement.getDevice());
  }

  protected String getSelector(int address){
    return properties.getSelector();
  }

  private DeviceSessionManagement createDeviceContext(DeviceHandler deviceHandler) {
    String filterName = properties.getFilter();
    DataFilter filter = DataFilter.ON_CHANGE;
    if (filterName != null) {
      filter = DataFilter.valueOf(filterName);
    }
    String selector = getSelector(0);
    DeviceSessionManagement context = new DeviceSessionManagement(deviceHandler, topicNameTemplate, filter, this, selector);
    context.setSession(hardwareManager.getSession());
    context.setHardwareManager(hardwareManager);
    context.setTransformation(TransformationManager.getInstance().getTransformation(
        deviceHandler.getBusName(),
        "" + deviceHandler.getBusNumber(),
        "" + deviceHandler.getDeviceAddress(),
        "anonymous"));
    return context;
  }

  public void deviceDetected(DeviceHandler deviceHandler) throws ExecutionException, InterruptedException {
    DeviceSessionManagement deviceSessionManagement = createDeviceContext(deviceHandler);
    deviceSessionCreated(deviceSessionManagement);
    try {
      deviceSessionManagement.start();
    } catch (ExecutionException | InterruptedException exception) {
      deviceSessionCreationFailed(deviceSessionManagement);
      throw exception;
    }
  }

  protected void deviceSessionCreated(DeviceSessionManagement deviceSessionManagement) {
    activeSessions.put(deviceSessionManagement.getDevice().getKey(), deviceSessionManagement);
  }

  protected void deviceSessionCreationFailed(DeviceSessionManagement deviceSessionManagement) {
    closedSession(deviceSessionManagement);
  }

  protected abstract DeviceHandler createDeviceHander(String key, DeviceController controller);

  protected abstract  Map<String, DeviceController> scan();

  @Override
  public void run() {
    Map<String, DeviceController> map = scan();
    for (Map.Entry<String, DeviceController> entry : map.entrySet()) {
      if (!foundDevices.containsKey(entry.getKey())) {
        // Found new device
        DeviceHandler handler = createDeviceHander(entry.getKey(), entry.getValue());
        handler.setTrigger(trigger);
        foundDevices.put(entry.getKey(), handler);
        try {
          deviceDetected(handler);
        } catch (ExecutionException e) {
          logger.log(DEVICE_MANAGER_MOUNT_FAILED, entry.getKey(), e);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
