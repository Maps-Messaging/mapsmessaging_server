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

import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.ServerPublishPacket;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static io.mapsmessaging.logging.ServerLogMessages.*;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class FrameHandler {

  private static final int MAX_FRAMES_PER_SELECTION = 100;
  private static final int MAX_PACKET_CAPACITY = Integer.MAX_VALUE - 8;

  private final WriteTask writeTask;
  private final int bufferSize;
  private Packet packet;
  private boolean isRegistered;
  private final Deque<ServerPacket> completedFrames;
  private final Deque<Packet> pendingWrites;

  public FrameHandler(WriteTask task, int bufferSize) {
    this.writeTask = task;
    this.bufferSize = bufferSize;
    completedFrames = new LinkedList<>();
    pendingWrites = new LinkedList<>();
    isRegistered = false;
    packet = new Packet(bufferSize, false);
  }

  public void processSelection() {
    int processedFrames = 0;
    while (processedFrames < MAX_FRAMES_PER_SELECTION) {
      ProcessResult processResult;
      synchronized (this) {
        processResult = processSelectionLocked(MAX_FRAMES_PER_SELECTION - processedFrames);
      }

      completeFrames(processResult.completedFrames());
      processedFrames += processResult.processedFrames();
      if (!processResult.continueProcessing()) {
        return;
      }
    }

    synchronized (this) {
      requestWriteCallback();
    }
  }

  private ProcessResult processSelectionLocked(int maximumFrames) {
    int packedFrames = 0;
    if (pendingWrites.isEmpty()) {
      try {
        packedFrames = packPacket(maximumFrames);
      } catch (RuntimeException runtimeException) {
        writeTask.logger.log(WRITE_TASK_SEND_FAILED, runtimeException);
        closeConnection();
        clearPendingState();
        cancelWrite();
        return ProcessResult.stop(0);
      }
      if (packedFrames == 0) {
        cancelIfIdle();
        return ProcessResult.stop(0);
      }
    }

    WriteResult result = writePendingPackets();
    if (result == WriteResult.FAILED) {
      clearPendingState();
      cancelWrite();
      return ProcessResult.stop(packedFrames);
    }
    if (result == WriteResult.BLOCKED) {
      requestWriteCallback();
      return ProcessResult.stop(packedFrames);
    }

    resetPacket();
    List<ServerPacket> framesToComplete = drainCompletedFrames();
    if (writeTask.outboundFrame.isEmpty()) {
      cancelIfIdle();
      return ProcessResult.stop(packedFrames, framesToComplete);
    }
    return ProcessResult.continueWith(packedFrames, framesToComplete);
  }

  private int packPacket(int maximumFrames) {
    ServerPacket serverPacket = writeTask.outboundFrame.poll();
    if (serverPacket == null) {
      return 0;
    }

    if (serverPacket instanceof ServerPublishPacket serverPublishPacket) {
      packAdvancedPacket(serverPacket, serverPublishPacket);
      return 1;
    }

    int count = 0;
    int coalesceLimit = Math.min(maximumFrames, Math.max(1, writeTask.getCoalesceSize()));
    while (serverPacket != null && count < coalesceLimit) {
      int startPos = packet.position();
      try {
        serverPacket.packFrame(packet);
        completedFrames.add(serverPacket);
        count++;
      } catch (BufferOverflowException overflow) {
        writeTask.selectorCallback.getEndPoint().getEndPointStatus().incrementOverFlow();
        packet.position(startPos);
        if (count == 0) {
          growPacket();
          continue;
        }
        writeTask.setCoalesceSize(count);
        writeTask.outboundFrame.addFirst(serverPacket);
        break;
      }

      if (count >= coalesceLimit) {
        break;
      }
      ServerPacket nextPacket = writeTask.outboundFrame.peek();
      if (nextPacket instanceof ServerPublishPacket) {
        break;
      }
      serverPacket = writeTask.outboundFrame.poll();
    }
    packet.flip();
    if (packet.hasRemaining()) {
      pendingWrites.add(packet);
    }
    return count;
  }

  private void packAdvancedPacket(ServerPacket serverPacket, ServerPublishPacket serverPublishPacket) {
    Packet[] packets;
    for (;;) {
      try {
        packets = serverPublishPacket.packAdvancedFrame(packet);
        break;
      } catch (BufferOverflowException overflow) {
        writeTask.selectorCallback.getEndPoint().getEndPointStatus().incrementOverFlow();
        growPacket();
      }
    }
    if (packets == null || packets.length == 0 || packets[0] != packet) {
      throw new IllegalStateException("Advanced frame must return the supplied header packet first");
    }
    packets[0].flip();
    for (Packet packetPart : packets) {
      if (packetPart != null && packetPart.hasRemaining()) {
        pendingWrites.add(packetPart);
      }
    }
    completedFrames.add(serverPacket);
  }

  private void growPacket() {
    int currentCapacity = packet.capacity();
    if (currentCapacity >= MAX_PACKET_CAPACITY) {
      throw new IllegalStateException("Frame exceeds maximum packet capacity");
    }
    int nextCapacity = currentCapacity <= MAX_PACKET_CAPACITY / 2
        ? Math.max(1, currentCapacity * 2)
        : MAX_PACKET_CAPACITY;
    packet = new Packet(nextCapacity, false);
  }

  public synchronized void registerWrite() {
    if (!isRegistered) {
      isRegistered = true;
      try {
        writeTask.logger.log(WRITE_TASK_WRITE);
        writeTask.selectorTask.register(OP_WRITE);
      } catch (IOException | RuntimeException e) {
        isRegistered = false;
        writeTask.logger.log(WRITE_TASK_UNABLE_TO_ADD_WRITE, e);
        closeConnection();
      }
    }
  }

  private synchronized void cancelIfIdle() {
    if (!pendingWrites.isEmpty() || !completedFrames.isEmpty() || !writeTask.outboundFrame.isEmpty()) {
      return;
    }
    cancelWrite();
  }

  /**
   * Cancels write interest for transports, such as UDP, that manage frame
   * serialization themselves but share this handler's registration lifecycle.
   */
  public synchronized void cancel() {
    cancelWrite();
  }

  /**
   * Retains the transport-facing flush hook exposed by the original handler.
   */
  public synchronized void writeBuffer() {
    writeBuffer(packet);
  }

  private void cancelWrite() {
    isRegistered = false;
    try {
      writeTask.logger.log(WRITE_TASK_WRITE_CANCEL);
      writeTask.selectorTask.cancel(OP_WRITE);
    } catch (IOException | RuntimeException e) {
      writeTask.logger.log(WRITE_TASK_UNABLE_TO_ADD_WRITE, e);
    }
  }

  private WriteResult writePendingPackets() {
    Packet packetToSend = pendingWrites.peek();
    while (packetToSend != null) {
      int before = packetToSend.position();
      if (!writeBuffer(packetToSend)) {
        return WriteResult.FAILED;
      }
      if (packetToSend.hasRemaining()) {
        return WriteResult.BLOCKED;
      }
      if (packetToSend.position() == before) {
        return WriteResult.BLOCKED;
      }
      pendingWrites.poll();
      packetToSend = pendingWrites.peek();
    }
    return WriteResult.COMPLETE;
  }

  private boolean writeBuffer(Packet packetToSend) {
    try {
      writeTask.logger.log(ServerLogMessages.WRITE_TASK_WRITE_PACKET, packetToSend);
      if (writeTask.selectorCallback.getEndPoint().sendPacket(packetToSend) == 0) {
        writeTask.logger.log(WRITE_TASK_BLOCKED);
      }
      return true;
    } catch (IOException | RuntimeException e) {
      closeConnection();
      writeTask.logger.log(WRITE_TASK_SEND_FAILED, e);
      return false;
    }
  }

  private List<ServerPacket> drainCompletedFrames() {
    List<ServerPacket> framesToComplete = new ArrayList<>(completedFrames.size());
    ServerPacket completedFrame = completedFrames.poll();
    while (completedFrame != null) {
      framesToComplete.add(completedFrame);
      completedFrame = completedFrames.poll();
    }
    return framesToComplete;
  }

  private void completeFrames(List<ServerPacket> framesToComplete) {
    for (ServerPacket completedFrame : framesToComplete) {
      try {
        completedFrame.complete();
      } catch (RuntimeException runtimeException) {
        writeTask.logger.log(WRITE_TASK_SEND_FAILED, runtimeException);
      }
    }
  }

  private void clearPendingState() {
    pendingWrites.clear();
    completedFrames.clear();
    resetPacket();
  }

  private void resetPacket() {
    if (packet.capacity() == bufferSize) {
      packet.clear();
    } else {
      packet = new Packet(bufferSize, false);
    }
  }

  private void requestWriteCallback() {
    if (!isRegistered) {
      return;
    }
    try {
      writeTask.selectorTask.register(OP_WRITE);
    } catch (IOException | RuntimeException e) {
      isRegistered = false;
      writeTask.logger.log(WRITE_TASK_UNABLE_TO_ADD_WRITE, e);
      closeConnection();
    }
  }

  private void closeConnection() {
    try {
      writeTask.selectorCallback.close();
    } catch (IOException | RuntimeException ioException) {
      writeTask.logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, ioException);
    }
  }

  private record ProcessResult(int processedFrames, List<ServerPacket> completedFrames, boolean continueProcessing) {

    private static ProcessResult stop(int processedFrames) {
      return stop(processedFrames, List.of());
    }

    private static ProcessResult stop(int processedFrames, List<ServerPacket> completedFrames) {
      return new ProcessResult(processedFrames, completedFrames, false);
    }

    private static ProcessResult continueWith(int processedFrames, List<ServerPacket> completedFrames) {
      return new ProcessResult(processedFrames, completedFrames, true);
    }
  }

  private enum WriteResult {
    COMPLETE,
    BLOCKED,
    FAILED
  }
}
