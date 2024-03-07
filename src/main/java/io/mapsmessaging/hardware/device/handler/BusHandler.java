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

package io.mapsmessaging.hardware.device.handler;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.hardware.device.DeviceClientConnection;
import io.mapsmessaging.hardware.device.DeviceSessionManagement;
import io.mapsmessaging.hardware.device.filter.DataFilter;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public abstract class BusHandler implements Runnable {
  private final Map<String, DeviceSessionManagement> activeSessions;
  private final Map<String, DeviceHandler> foundDevices;
  protected final ConfigurationProperties properties;
  private final int scanPeriod;
  private Future<?> scheduledFuture;
  private final Trigger trigger;
  private final String topicNameTemplate;


  protected BusHandler(ConfigurationProperties properties, Trigger trigger){
    foundDevices = new ConcurrentHashMap<>();
    activeSessions = new ConcurrentHashMap<>();
    this.properties = properties;
    this.trigger = trigger;
    scanPeriod = properties.getIntProperty("scanTime", 120000);
    topicNameTemplate = properties.getProperty("topicNameTemplate", "/device/[bus_name]/[bus_number]/[device_addr]/[device_name]");
  }

  public synchronized void start() {
    scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, 5000, scanPeriod, TimeUnit.MILLISECONDS);
  }

  public synchronized void stop(){
    if(scheduledFuture != null){
      scheduledFuture.cancel(true);
    }
  }

  public void pause() {
  }

  public void resume() {
  }

  private SessionContext createContext(DeviceHandler deviceHandler){
    SessionContextBuilder builder = new SessionContextBuilder(deviceHandler.getBusName()+"_"+deviceHandler.getName(), new DeviceClientConnection(deviceHandler));
    builder.setPersistentSession(false)
    .setKeepAlive(0)
    .setResetState(true)
    .setUsername("anonymous")
    .setPassword(new char[0])
    .setReceiveMaximum(10);
    return builder.build();
  }

  public void closedSession(DeviceSessionManagement deviceSessionManagement){
    activeSessions.remove(deviceSessionManagement.getName());
    foundDevices.remove(deviceSessionManagement.getDevice().getKey());
  }

  protected String getSelector(int address){
    return properties.getProperty("selector", "");
  }

  private DeviceSessionManagement createSession(DeviceHandler deviceHandler) {
    String filterName  = properties.getProperty("filter", "ON_CHANGE");
    DataFilter filter = DataFilter.valueOf(filterName);
    if(filter == null){
      filter = DataFilter.ON_CHANGE;
    }
    String selector = getSelector(deviceHandler.getDeviceAddress());
    DeviceSessionManagement deviceSessionManagement = new DeviceSessionManagement(deviceHandler, topicNameTemplate, filter, this, selector);
    SessionContext context = createContext(deviceHandler);
    CompletableFuture<Session> future = SessionManager.getInstance().createAsync(context, deviceSessionManagement);
    future.thenApply(session -> {
      try {
        session.login();
        deviceSessionManagement.setSession(session);
        deviceSessionManagement.setTransformation((TransformationManager.getInstance().getTransformation(deviceHandler.getName(), session.getSecurityContext().getUsername())));
        return session;
      }
      catch(IOException failedLogin){
        // To Do
      }
      return session;
    });
    return deviceSessionManagement;
  }

  public void deviceDetected(DeviceHandler deviceHandler) throws ExecutionException, InterruptedException {
    DeviceSessionManagement deviceSessionManagement = createSession(deviceHandler);
    activeSessions.put(deviceSessionManagement.getName(), deviceSessionManagement);
    deviceSessionManagement.start();
  }

  protected abstract DeviceHandler createDeviceHander(String key, DeviceController controller);

  protected abstract  Map<String, DeviceController> scan();

  @SneakyThrows
  @Override
  public void run() {
    Map<String, DeviceController> map = scan();
    for (Map.Entry<String, DeviceController> entry : map.entrySet()) {
      if (!foundDevices.containsKey(entry.getKey())) {
        // Found new device
        DeviceHandler handler = createDeviceHander(entry.getKey(), entry.getValue());
        handler.setTrigger(trigger);
        foundDevices.put(entry.getKey(), handler);
        deviceDetected(handler);
      }
    }
  }
}
