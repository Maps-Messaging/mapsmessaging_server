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

package io.mapsmessaging.network.io.impl.lora.serial;

import io.mapsmessaging.config.network.impl.LoRaSerialDeviceConfig;
import io.mapsmessaging.network.io.impl.lora.LoRaDevice;
import io.mapsmessaging.network.protocol.impl.loragateway.LoRaProtocol;
import lombok.Getter;

public class LoRaSerialDevice extends LoRaDevice {
  @Getter
  private LoRaProtocol activeProtocol;

  public LoRaSerialDevice(LoRaSerialDeviceConfig config) {
    super(config);
    activeProtocol = null;
    isInitialised = false;
  }


  @Override
  public void close() {

  }

  public void setProtocol(LoRaProtocol loRaProtocol) {
    activeProtocol = loRaProtocol;
  }
}
