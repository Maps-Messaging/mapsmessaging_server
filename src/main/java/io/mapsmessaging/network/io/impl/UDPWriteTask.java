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

package io.mapsmessaging.network.io.impl;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;

import java.io.IOException;

import static io.mapsmessaging.logging.ServerLogMessages.UDP_WRITE_TASK_SEND_PACKET_ERROR;
import static io.mapsmessaging.logging.ServerLogMessages.UDP_WRITE_TASK_SENT_PACKET;

public class UDPWriteTask extends WriteTask {

  private final int bufferSize;

  public UDPWriteTask(SelectorCallback selectorCallback, int bufferSize, SelectorTask selectorTask, Logger logger) {
    super(selectorCallback, bufferSize, selectorTask, logger);
    this.bufferSize = bufferSize;
  }

  @Override
  public void handleWrite() {
    Packet packet = new Packet(bufferSize, false);
    ServerPacket frame = outboundFrame.poll();
    if (frame != null) {
      packet.setFromAddress(frame.getFromAddress());
      frame.packFrame(packet);
      packet.flip();
      try {
        selectorCallback.getEndPoint().sendPacket(packet);
        frame.complete();
        logger.log(UDP_WRITE_TASK_SENT_PACKET, packet);
      } catch (IOException e) {
        logger.log(UDP_WRITE_TASK_SEND_PACKET_ERROR, e);
      }
      if (outboundFrame.isEmpty()) {
        frameHandler.cancel();
      }
    }
  }
}
