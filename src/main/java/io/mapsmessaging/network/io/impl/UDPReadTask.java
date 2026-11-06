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

package io.mapsmessaging.network.io.impl;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.Packet;

import java.io.IOException;

import static io.mapsmessaging.logging.ServerLogMessages.READ_TASK_COMPLETED;
import static io.mapsmessaging.logging.ServerLogMessages.UDP_READ_TASK_READ_PACKET;
import static io.mapsmessaging.logging.ServerLogMessages.UDP_READ_TASK_STATE;

public class UDPReadTask extends ReadTask {

  private final Packet udpPacket;

  public UDPReadTask(SelectorCallback selectorCallback, int bufferSize, long threshold, Logger logger) {
    super(selectorCallback, bufferSize, logger, -1, -1);
    udpPacket = new Packet(bufferSize, false);
  }

  @Override
  public void read() throws IOException {
    udpPacket.clear();
    int len = endPoint.readPacket(udpPacket);
    logger.log(UDP_READ_TASK_STATE, udpPacket.getFromAddress(), len);
    logger.log(READ_TASK_COMPLETED, udpPacket.position(), udpPacket.limit(), len);
    if (len > 0) {
      udpPacket.flip();
      logger.log(UDP_READ_TASK_READ_PACKET, udpPacket);
      selectorCallback.processPacket(udpPacket);
    }
  }
}
