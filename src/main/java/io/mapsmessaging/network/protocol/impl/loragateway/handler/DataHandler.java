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
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.loragateway.LoRaProtocol;

import java.io.IOException;

public class DataHandler implements PacketHandler {

  @Override
  public boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) throws IOException {
    int loraClientId = packet.get();
    int rssi = packet.get();
    packet.getRawBuffer().compact();
    packet.position(0);
    packet.limit(len - 2);
    packet.getRawBuffer().limit(len - 2);
    packet.setFromAddress(loRaProtocol.getSocketAddress(loraClientId));
    loRaProtocol.handleIncomingPacket(packet, loraClientId, rssi);
    return false;
  }
}
