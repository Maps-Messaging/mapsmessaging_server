/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.network.io.impl;

import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static io.mapsmessaging.logging.ServerLogMessages.*;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class FrameHandler {
  private final WriteTask writeTask;
  private final Packet packet;
  private boolean isRegistered;
  private final Queue<ServerPacket> completedFrames;

  public FrameHandler(WriteTask task, int bufferSize) {
    this.writeTask = task;
    completedFrames = new LinkedList<>();
    isRegistered = false;
    packet = new Packet(bufferSize, false);
  }

  public void processSelection() {
    if (!packet.hasData()) {
      int count = 0;
      ServerPacket serverPacket = writeTask.outboundFrame.poll();
      while (count < writeTask.getCoalesceSize() && serverPacket != null) {
        int startPos = packet.position();
        try {
          serverPacket.packFrame(packet);
          completedFrames.add(serverPacket);
          count++;
        } catch (BufferOverflowException overflow) {
          writeTask.selectorCallback.getEndPoint().incrementOverFlow();
          writeTask.setCoalesceSize( count );
          packet.position(startPos);
          ((ConcurrentLinkedDeque)writeTask.outboundFrame).addFirst(serverPacket);
          serverPacket = null;
          count =  writeTask.getCoalesceSize();
        }
        if (count <  writeTask.getCoalesceSize()) {
          serverPacket =  writeTask.outboundFrame.poll();
        }
      }
      packet.flip();
    }
    // Outstanding data in packet so lets empty it
    writeBuffer();
    if (!packet.hasData()) {
      packet.clear();
      while (!completedFrames.isEmpty()) {
        completedFrames.poll().complete();
      }
      // Completed the packet and the queue is empty, so cancel the write
      if ( writeTask.outboundFrame.isEmpty()) {
        cancel();
      }
    }
  }

  public synchronized void registerWrite() {
    if (!isRegistered) {
      isRegistered = true;
      try {
        writeTask.logger.log(WRITE_TASK_WRITE);
        writeTask.selectorTask.register(OP_WRITE);
      } catch (IOException e) {
        writeTask.logger.log(WRITE_TASK_UNABLE_TO_ADD_WRITE);
      }
    }
  }

  public synchronized void cancel() {
    isRegistered = false;
    try {
      writeTask.logger.log(WRITE_TASK_WRITE_CANCEL);
      writeTask.selectorTask.cancel(OP_WRITE);
    } catch (IOException e) {
      writeTask.logger.log(WRITE_TASK_UNABLE_TO_ADD_WRITE);
    }
  }

  public boolean writeBuffer() {
    try {
      writeTask.logger.log(ServerLogMessages.WRITE_TASK_WRITE_PACKET, packet);
      if ( writeTask.selectorCallback.getEndPoint().sendPacket(packet) == 0) {
        writeTask.logger.log(WRITE_TASK_BLOCKED);
        return false;
      }
    } catch (IOException e) {
      try {
        writeTask.selectorCallback.close();
      } catch (IOException ioException) {
        writeTask.logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
      writeTask.logger.log(WRITE_TASK_SEND_FAILED, e);
    }
    return true;
  }
}