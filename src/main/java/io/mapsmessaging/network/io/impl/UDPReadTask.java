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

package io.mapsmessaging.network.io.impl;

import static io.mapsmessaging.logging.LogMessages.READ_TASK_COMPLETED;
import static io.mapsmessaging.logging.LogMessages.UDP_READ_TASK_READ_PACKET;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.Packet;
import java.io.IOException;

public class UDPReadTask extends ReadTask {
  private final Packet udpPacket;


  public UDPReadTask(SelectorCallback selectorCallback, int bufferSize, Logger logger) {
    super(selectorCallback, bufferSize, logger, -1, -1);
    udpPacket = new Packet(bufferSize, false);
  }

  @Override
  public void read() throws IOException {
    udpPacket.clear();
    int len = endPoint.readPacket(udpPacket);
    logger.log(READ_TASK_COMPLETED, packet.position(), packet.limit(), len);
    if (len > 0) {
      udpPacket.flip();
      logger.log(UDP_READ_TASK_READ_PACKET, udpPacket);
      selectorCallback.processPacket(udpPacket);
    }
  }
}
