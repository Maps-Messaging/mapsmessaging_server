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

public class LoRaDeviceConfigBuilder {

  private LoRaDeviceConfig config;


  public LoRaDeviceConfigBuilder(){
    config = new LoRaDeviceConfig();
  }

  public boolean isValid() {
    return (config.getCs() != -1 && config.getIrq() != -1 && config.getRst() != -1);
  }

  public LoRaDeviceConfig build(){
    return config;
  }

  protected LoRaDeviceConfigBuilder setName(String name) {
    config.setName(name);
    return this;
  }

  protected LoRaDeviceConfigBuilder setRadio(String radio) {
    config.setRadio(radio);
    return this;
  }

  protected LoRaDeviceConfigBuilder setCs(int cs) {
    config.setCs(cs);
    return this;
  }

  protected LoRaDeviceConfigBuilder setIrq(int irq) {
    config.setIrq(irq);
    return this;
  }

  protected LoRaDeviceConfigBuilder setRst(int rst) {
    config.setRst(rst);
    return this;
  }

  protected LoRaDeviceConfigBuilder setPower(int power) {
    config.setPower(power);
    return this;
  }

  protected LoRaDeviceConfigBuilder setCadTimeout(int cadTimeout) {
    config.setCadTimeout(cadTimeout);
    return this;
  }

  protected LoRaDeviceConfigBuilder setFrequency(float frequency) {
    config.setFrequency(frequency);
    return this;
  }
}
