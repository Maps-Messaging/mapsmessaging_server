/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.io.impl;

import static org.maps.logging.LogMessages.UDP_WRITE_TASK_SEND_PACKET_ERROR;
import static org.maps.logging.LogMessages.UDP_WRITE_TASK_SENT_PACKET;

import java.io.IOException;
import org.maps.logging.Logger;
import org.maps.network.io.Packet;
import org.maps.network.io.ServerPacket;

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
