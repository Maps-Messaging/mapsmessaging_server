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

package org.maps.network.protocol.impl.loragateway.handler;

import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.loragateway.LoRaProtocol;

public class SuccessHandler implements PacketHandler {

  @Override
  public boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) {
    if (len != 0) {
      logger.log(LogMessages.LORA_GATEWAY_SUCCESS, Integer.toHexString(packet.get()));
    } else {
      logger.log(LogMessages.LORA_GATEWAY_SUCCESS);
    }
    return true;
  }
}
