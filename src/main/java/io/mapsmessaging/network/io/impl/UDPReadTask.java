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
import lombok.Data;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class UDPReadTask extends ReadTask {

  private final Map<SocketAddress, OutstandingPacket> outstandingPacketMap;
  private final long threshold;
  private Packet udpPacket;

  public UDPReadTask(SelectorCallback selectorCallback, int bufferSize, long threshold, Logger logger) {
    super(selectorCallback, bufferSize, logger, -1, -1);
    udpPacket = new Packet(bufferSize, false);
    this.threshold = threshold;
    outstandingPacketMap = new LinkedHashMap<>();
  }

  @Override
  public void read() throws IOException {
    removeOldPackets(threshold);
    udpPacket.clear();
    int len = endPoint.readPacket(udpPacket);
    logger.log(UDP_READ_TASK_STATE, udpPacket.getFromAddress(), len);
    OutstandingPacket previous = outstandingPacketMap.remove(packet.getFromAddress());
    if (previous != null) {
      Packet pkt = previous.getPacket();
      logger.log(UDP_READ_TASK_STATE_PREVIOUS, pkt.getFromAddress(), pkt.available());
      pkt.getRawBuffer().put(udpPacket.getRawBuffer());
      udpPacket = pkt;
      logger.log(UDP_READ_TASK_STATE_RECOMBINED, udpPacket.available(), udpPacket.available());
    }
    logger.log(READ_TASK_COMPLETED, packet.position(), packet.limit(), len);
    if (len > 0) {
      udpPacket.flip();
      logger.log(UDP_READ_TASK_READ_PACKET, udpPacket);
      selectorCallback.processPacket(udpPacket);
      if (udpPacket.hasRemaining()) {
        udpPacket.compact();
        udpPacket.flip();
        logger.log(UDP_READ_TASK_STATE_REMAINING, udpPacket.getFromAddress(), udpPacket.available());
        outstandingPacketMap.put(udpPacket.getFromAddress(), new OutstandingPacket(udpPacket));
      }
    }
  }

  public void removeOldPackets(long ageThreshold) {
    long currentTime = System.currentTimeMillis();
    for (Iterator<Map.Entry<SocketAddress, OutstandingPacket>> it =
         outstandingPacketMap.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<SocketAddress, OutstandingPacket> entry = it.next();
      OutstandingPacket packet = entry.getValue();
      if (currentTime - packet.getAge() > ageThreshold) {
        it.remove();
      }
    }
  }

  @Data
  private static class OutstandingPacket {
    Packet packet;
    long age;

    public OutstandingPacket(Packet outstanding) {
      // Create a duplicate of the original ByteBuffer
      ByteBuffer duplicate = outstanding.getRawBuffer().duplicate();
      duplicate.clear();

      // Copy data from the original to the duplicate
      duplicate.put(outstanding.getRawBuffer());
      packet = new Packet(duplicate);
      packet.flip();
      age = System.currentTimeMillis();
    }
  }
}
