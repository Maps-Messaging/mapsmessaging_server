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

package io.mapsmessaging.network.protocol.impl.loragateway.handler;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.loragateway.LoRaProtocol;

import java.io.IOException;

import static io.mapsmessaging.network.protocol.impl.loragateway.Constants.*;

public class PingHandler implements PacketHandler {

  @Override
  public boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) throws IOException {
    int state = packet.get();
    logger.log(ServerLogMessages.LORA_GATEWAY_PING, state);
    if (!loRaProtocol.isSentVersion()) {
      loRaProtocol.sendCommand(VERSION); // Request version of gateway
    } else if (loRaProtocol.isSentConfig() && !loRaProtocol.isStarted()) {
      loRaProtocol.sendCommand(START, (byte) 0, null);
      loRaProtocol.setStarted(true);
    } else {
      loRaProtocol.sendCommand(PING);
    }
    return true;
  }
}
