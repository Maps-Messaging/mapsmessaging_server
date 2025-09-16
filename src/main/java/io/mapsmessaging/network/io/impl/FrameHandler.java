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

import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.ServerPublishPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.ConnAck;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.Deque;
import java.util.LinkedList;

import static io.mapsmessaging.logging.ServerLogMessages.*;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class FrameHandler {
  private final WriteTask writeTask;
  private final Packet packet;
  private boolean isRegistered;
  private final Deque<ServerPacket> completedFrames;

  public FrameHandler(WriteTask task, int bufferSize) {
    this.writeTask = task;
    completedFrames = new LinkedList<>();
    isRegistered = false;
    packet = new Packet(bufferSize, false);
  }

  public void processSelection() {
    boolean sent = packPacket();
    // Outstanding data in packet so lets empty it
    if(!sent) {
      writeBuffer();
    }
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

  private boolean packPacket(){
    boolean sent = false;
    if (!packet.hasData()) {
      int count = 0;
      ServerPacket serverPacket = writeTask.outboundFrame.poll();
      while (count < writeTask.getCoalesceSize() && serverPacket != null) {
        int startPos = packet.position();
        try {
          sent = processPacket(serverPacket);
          completedFrames.add(serverPacket);
          count++;
        } catch (BufferOverflowException overflow) {
          writeTask.selectorCallback.getEndPoint().getEndPointStatus().incrementOverFlow();
          writeTask.setCoalesceSize( count );
          packet.position(startPos);
          writeTask.outboundFrame.addFirst(serverPacket);
          serverPacket = null;
          count =  writeTask.getCoalesceSize();
        }
        if (count <  writeTask.getCoalesceSize()) {
          serverPacket =  writeTask.outboundFrame.poll();
        }
      }
      if(!sent) {
        packet.flip();
      }
    }
    return sent;
  }

  private boolean processPacket(ServerPacket serverPacket){
    boolean sent = false;
    if(serverPacket instanceof ServerPublishPacket serverPublishPacket){
      Packet[] packets = serverPublishPacket.packAdvancedFrame(packet);
      packets[0].flip();
      for(Packet packetParts:packets){
        writeBuffer(packetParts);
      }
      sent = true;
    }
    else {
      serverPacket.packFrame(packet);
    }
    return sent;
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

  public void writeBuffer(){
    writeBuffer(packet);
  }

  private void writeBuffer(Packet packetToSend) {
    try {
      writeTask.logger.log(ServerLogMessages.WRITE_TASK_WRITE_PACKET, packet);
      if ( writeTask.selectorCallback.getEndPoint().sendPacket(packetToSend) == 0) {
        writeTask.logger.log(WRITE_TASK_BLOCKED);
      }
    } catch (IOException e) {
      try {
        writeTask.selectorCallback.close();
      } catch (IOException ioException) {
        writeTask.logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
      writeTask.logger.log(WRITE_TASK_SEND_FAILED, e);
    }
  }
}