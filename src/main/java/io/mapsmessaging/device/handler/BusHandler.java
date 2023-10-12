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

import io.mapsmessaging.device.handler.onewire.OneWireDeviceHandler;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class BusHandler implements Runnable {

  private final Map<String, DeviceHandler> foundDevices;
  protected final ConfigurationProperties properties;
  private final int scanPeriod;
  private Future<?> scheduledFuture;

  protected BusHandler(ConfigurationProperties properties){
    foundDevices = new ConcurrentHashMap<>();
    this.properties = properties;
    scanPeriod = properties.getIntProperty("scanTime", 120000);
  }

  public synchronized void start() {
    if(properties.getBooleanProperty("autoScan", false)) {
      scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, scanPeriod, scanPeriod, TimeUnit.MILLISECONDS);
    }
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

  public abstract void deviceDetected(DeviceHandler deviceHandler);

  protected abstract  Map<String, DeviceController> scan();

  @Override
  public void run() {
    Map<String, DeviceController> map = scan();
    for (Map.Entry<String, DeviceController> entry : map.entrySet()) {
      if (!foundDevices.containsKey(entry.getKey())) {
        // Found new device
        DeviceHandler handler = new OneWireDeviceHandler(entry.getValue());
        foundDevices.put(entry.getKey(), handler);
        deviceDetected(handler);
      }
    }
  }
}
