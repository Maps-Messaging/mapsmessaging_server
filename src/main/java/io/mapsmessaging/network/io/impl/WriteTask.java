/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

import static io.mapsmessaging.logging.ServerLogMessages.WRITE_TASK_BLOCKED;
import static io.mapsmessaging.logging.ServerLogMessages.WRITE_TASK_SEND_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.WRITE_TASK_UNABLE_TO_ADD_WRITE;
import static io.mapsmessaging.logging.ServerLogMessages.WRITE_TASK_WRITE;
import static io.mapsmessaging.logging.ServerLogMessages.WRITE_TASK_WRITE_CANCEL;
import static java.nio.channels.SelectionKey.OP_WRITE;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.ServerPacket;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WriteTask implements Selectable {

  final Logger logger;
  final SelectorTask selectorTask;
  final Queue<ServerPacket> outboundFrame;
  final FrameHandler frameHandler;
  final SelectorCallback selectorCallback;
  int coalesceSize;

  public WriteTask(SelectorCallback selectorCallback, int bufferSize, SelectorTask selectorTask, Logger logger) {
    this.selectorTask = selectorTask;
    this.selectorCallback = selectorCallback;
    this.logger = logger;
    frameHandler = new FrameHandler(bufferSize);
    outboundFrame = new ConcurrentLinkedDeque<>();
    coalesceSize = 100;
  }

  public int getCoalesceSize() {
    return coalesceSize;
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    ThreadContext.put("endpoint", selectorCallback.getEndPoint().getName());
    ThreadContext.put("protocol", selectorCallback.getName());
    ThreadContext.put("session", selectorCallback.getSessionId());
    ThreadContext.put("version", selectorCallback.getVersion());
    try {
      handleWrite();
    } catch (Exception e) {
      logger.log(WRITE_TASK_SEND_FAILED, e);
    }
    ThreadContext.clearMap();
  }

  public void handleWrite() {
    frameHandler.processSelection();
  }

  public void push(ServerPacket frame) {
    outboundFrame.offer(frame);
    frameHandler.registerWrite();
  }

  public int size() {
    return outboundFrame.size();
  }

  class FrameHandler {

    private final Packet packet;
    boolean isRegistered;
    private final Queue<ServerPacket> completedFrames;

    public FrameHandler(int bufferSize) {
      completedFrames = new LinkedList<>();
      isRegistered = false;
      packet = new Packet(bufferSize, false);
    }

    private void processSelection() {
      if (!packet.hasData()) {
        int count = 0;
        ServerPacket serverPacket = outboundFrame.poll();
        while (count < coalesceSize && serverPacket != null) {
          int startPos = packet.position();
          try {
            serverPacket.packFrame(packet);
            completedFrames.add(serverPacket);
            count++;
          } catch (BufferOverflowException overflow) {
            selectorCallback.getEndPoint().incrementOverFlow();
            coalesceSize = count;
            packet.position(startPos);
            ((LinkedList<ServerPacket>) outboundFrame).addFirst(serverPacket);
            serverPacket = null;
            count = coalesceSize;
          }
          if (count < coalesceSize) {
            serverPacket = outboundFrame.poll();
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
        if (outboundFrame.isEmpty()) {
          cancel();
        }
      }
    }

    private synchronized void registerWrite() {
      if (!isRegistered) {
        isRegistered = true;
        try {
          logger.log(WRITE_TASK_WRITE);
          selectorTask.register(OP_WRITE);
        } catch (IOException e) {
          logger.log(WRITE_TASK_UNABLE_TO_ADD_WRITE);
        }
      }
    }

    public synchronized void cancel() {
      isRegistered = false;
      try {
        logger.log(WRITE_TASK_WRITE_CANCEL);
        selectorTask.cancel(OP_WRITE);
      } catch (IOException e) {
        logger.log(WRITE_TASK_UNABLE_TO_ADD_WRITE);
      }
    }

    private boolean writeBuffer() {
      try {
        logger.log(ServerLogMessages.WRITE_TASK_WRITE_PACKET, packet);
        if (selectorCallback.getEndPoint().sendPacket(packet) == 0) {
          logger.log(WRITE_TASK_BLOCKED);
          return false;
        }
      } catch (IOException e) {
        try {
          selectorCallback.close();
        } catch (IOException ioException) {
          logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
        }
        logger.log(WRITE_TASK_SEND_FAILED, e);
      }
      return true;
    }
  }
}
