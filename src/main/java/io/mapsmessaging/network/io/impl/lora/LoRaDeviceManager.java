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

package io.mapsmessaging.network.io.impl.lora;

import io.mapsmessaging.config.LoRaDeviceManagerConfig;
import io.mapsmessaging.config.network.impl.LoRaChipDeviceConfig;
import io.mapsmessaging.config.network.impl.LoRaSerialDeviceConfig;
import io.mapsmessaging.dto.rest.config.network.impl.LoRaConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.impl.lora.device.LoRaChipDevice;
import io.mapsmessaging.network.io.impl.lora.serial.LoRaSerialDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class LoRaDeviceManager {

  private static class Holder {
    static final LoRaDeviceManager INSTANCE = new LoRaDeviceManager();
  }
  public static LoRaDeviceManager getInstance() {
    return Holder.INSTANCE;
  }

  //
  // Some devices can run up to 3 different radio devices, all independent of each other
  // The thing that makes them unique is the config to use to communicate with them
  // This list maintains the physical device list that can be reused by multiple EndPoints to
  // send / receive packets specific to their configured Node ID
  //
  private final List<LoRaDevice> physicalDevices = new ArrayList<>();
  private final AtomicBoolean active = new AtomicBoolean(false);
  private final LoRaDeviceManagerConfig deviceConfig;

  private LoRaDeviceManager() {
    synchronized (physicalDevices) {
      deviceConfig = LoRaDeviceManagerConfig.getInstance();
      boolean libLoaded = false;
      try {
        System.loadLibrary("LoRaChipDevice");
        libLoaded = true;
      } catch (UnsatisfiedLinkError e) {
        Logger logger = LoggerFactory.getLogger(LoRaDeviceManager.class);
        logger.log(ServerLogMessages.LORA_DEVICE_LIBRARY_NOT_LOADED, e.getMessage());
      }
      for (LoRaConfigDTO config : deviceConfig.getDeviceConfigList()) {
        LoRaDevice device = null;
        if (config instanceof LoRaChipDeviceConfig && libLoaded) {
          device = new LoRaChipDevice((LoRaChipDeviceConfig) config);
        } else if (config instanceof LoRaSerialDeviceConfig) {
          device = new LoRaSerialDevice((LoRaSerialDeviceConfig) config);
        }
        if (device != null) {
          physicalDevices.add(device);
        }
      }
      active.set(true);
    }
  }

  public List<LoRaDevice> getDevices() {
    return new ArrayList<>(physicalDevices);
  }

  public LoRaDevice getDevice(EndPointURL url) {
    synchronized (physicalDevices) {
      for (LoRaDevice device : physicalDevices) {
        if (device.getName().equals(url.getHost())) {
          return device;
        }
      }
      return null;
    }
  }

  public boolean isLoaded() {
    return active.get();
  }
}
