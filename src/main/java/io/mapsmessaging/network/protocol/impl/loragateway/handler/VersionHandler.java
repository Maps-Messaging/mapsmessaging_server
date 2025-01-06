/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.protocol.impl.loragateway.handler;

import static io.mapsmessaging.network.protocol.impl.loragateway.Constants.CONFIG;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.loragateway.LoRaProtocol;
import java.io.IOException;

public class VersionHandler implements PacketHandler {

  @Override
  public boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) throws IOException {
    loRaProtocol.setSentVersion(true);
    byte[] config = loRaProtocol.getConfigBuffer();
    loRaProtocol.sendCommand(CONFIG, (byte) config.length, config);
    loRaProtocol.setSentConfig(true);
    return true;
  }
}
