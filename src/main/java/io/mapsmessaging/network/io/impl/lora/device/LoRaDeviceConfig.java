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

package io.mapsmessaging.network.io.impl.lora.device;

public class LoRaDeviceConfig {

  private String name;
  private String radio;

  private int cs;
  private int irq;
  private int rst;
  private int power;
  private int cadTimeout;
  private float frequency;

  LoRaDeviceConfig() {
  }

  public String getName() {
    return name;
  }

  protected void setName(String name) {
    this.name = name;
  }

  public String getRadio() {
    return radio;
  }

  protected void setRadio(String radio) {
    this.radio = radio;
  }

  public int getCs() {
    return cs;
  }

  protected void setCs(int cs) {
    this.cs = cs;
  }

  public int getIrq() {
    return irq;
  }

  protected void setIrq(int irq) {
    this.irq = irq;
  }

  public int getRst() {
    return rst;
  }

  protected void setRst(int rst) {
    this.rst = rst;
  }

  public int getPower() {
    return power;
  }

  protected void setPower(int power) {
    this.power = power;
  }

  public int getCadTimeout() {
    return cadTimeout;
  }

  protected void setCadTimeout(int cadTimeout) {
    this.cadTimeout = cadTimeout;
  }

  public float getFrequency() {
    return frequency;
  }

  protected void setFrequency(float frequency) {
    this.frequency = frequency;
  }
}
