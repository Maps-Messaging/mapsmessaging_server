/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.io.impl.lora.device;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.maps.network.EndPointURL;
import org.maps.utilities.configuration.ConfigurationProperties;
import org.maps.utilities.configuration.ConfigurationManager;

public class LoRaDeviceManager {

  private static final LoRaDeviceManager instance = new LoRaDeviceManager();
  public static LoRaDeviceManager getInstance(){
    return instance;
  }


  //
  // Some devices can run up to 3 different radio devices, all independent of each other
  // The thing that makes them unique is the config to use to communicate with them
  // This list maintains the physical device list that can be reused by multiple EndPoints to
  // send / receive packets specific to their configured Node ID
  //
  private final List<LoRaDevice> physicalDevices = new ArrayList<>();

  private LoRaDeviceManager() {
    synchronized (physicalDevices) {
      try {
        System.loadLibrary("LoRaDevice");
        ConfigurationProperties configMap = ConfigurationManager.getInstance().getProperties("LoRaDevice");
        for (Object obj : configMap.values()) {
          if(obj instanceof ConfigurationProperties) {
            ConfigurationProperties properties = (ConfigurationProperties) obj;
            LoRaDeviceConfigBuilder configBuilder = new LoRaDeviceConfigBuilder();
            configBuilder.setName(properties.getProperty("name"))
                .setRadio(properties.getProperty("radio"))
                .setCs(properties.getIntProperty("cs", -1))
                .setIrq(properties.getIntProperty("irq", -1))
                .setRst(properties.getIntProperty("rst", -1))
                .setPower(properties.getIntProperty("power", 14))
                .setCadTimeout(properties.getIntProperty("CADTimeout", 0))
                .setFrequency(properties.getFloatProperty("frequency", 0.0f));
            if (configBuilder.isValid()) {
              LoRaDevice device = new LoRaDevice(configBuilder.build());
              physicalDevices.add(device);
            }
          }
        }
      } catch (UnsatisfiedLinkError e) {
        // No point in worrying if its not a Pi it will not load
      }
    }
  }

  public LoRaDevice getDevice(EndPointURL url){
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
    return !physicalDevices.isEmpty();
  }
}
