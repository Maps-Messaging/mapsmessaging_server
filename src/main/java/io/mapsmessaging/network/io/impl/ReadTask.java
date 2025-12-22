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
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.utilities.IpAddressHelper;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class ReadTask implements Selectable {

  protected final Logger logger;
  protected final Packet packet;
  protected final EndPoint endPoint;
  protected final SelectorCallback selectorCallback;
  private final int readDelay;
  private final int readFragmentation;

  private long underflow;

  public ReadTask(SelectorCallback selectorCallback, int bufferSize, Logger logger, int readDelay, int readFragmentation) {
    this.logger = logger;
    this.selectorCallback = selectorCallback;
    this.readDelay = readDelay;
    this.readFragmentation = readFragmentation;
    endPoint = selectorCallback.getEndPoint();
    packet = new Packet(bufferSize, true);
  }

  public void pushOutstandingData(Packet initialPacket) {
    if (initialPacket.hasRemaining()) {
      packet.put(initialPacket);
    }
  }

  void closeProtocol() {
    try {
      selectorCallback.close();
    } catch (IOException e) {
      logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    try {
      if (readDelay > 0 && underflow > readFragmentation) {
        underflow = 0;
        endPoint.deregister(SelectionKey.OP_READ);
        if (!endPoint.isClosed()) {
          SimpleTaskScheduler.getInstance().schedule(new ScheduledRead(selectable), readDelay, TimeUnit.MILLISECONDS);
        }
      } else {
        ThreadContext.put("endpoint", endPoint.getName());
        ThreadContext.put("ip", IpAddressHelper.normalizeIp(endPoint.getRemoteSocketAddress()));
        ThreadContext.put("protocol", selectorCallback.getName());
        ThreadContext.put("session", selectorCallback.getSessionId());
        ThreadContext.put("version", selectorCallback.getVersion());
        read();
        if (packet.position() == packet.capacity()) {
          packet.clear();
        }
      }
    } catch (IOException e) {
      if (!(e.getMessage().equalsIgnoreCase("Socket closed") || e.getMessage().equalsIgnoreCase("Connection reset"))) {
        logger.log(READ_TASK_PACKET_EXCEPTION, e);
      }
      closeProtocol();
    } catch (RuntimeException th) {
      logger.log(READ_TASK_EXCEPTION, th);
      closeProtocol();
    } finally {
      ThreadContext.clearMap();
    }
  }

  void handleDataToProcess(int response) throws IOException {
    packet.flip();
    logger.log(READ_TASK_READ_PROCESSING, response, packet);
    if (!selectorCallback.processPacket(packet) && packet.hasData()) {
      endPoint.getEndPointStatus().incrementUnderFlow();
      underflow++;
    }
    logger.log(READ_TASK_POST_PROCESSING, packet);
    //
    // If the position == limit then it means that all the data has been read and there is no
    // outstanding data to be processed. Basically the packet contained a complete protocol
    // event/frame
    //
    if (packet.position() == packet.limit()) {
      packet.clear();
    }

    //
    // If there is data left in the packet, it means that the protocol has not been able to
    // construct a complete event/frame from the packet and more data is required. In this case
    // lets compact the buffer ( moves the data to the start of the buffer ) and this should set up
    // for the next read to start at the end of the current data
    // This is important else we will either write over the buffer or ignore it in the next read
    // cycle
    //
    else if (packet.position() < packet.limit()) {
      if (packet.position() != 0) {
        packet.compact();
        logger.log(READ_TASK_COMPACT, packet);
      } else {
        packet.position(packet.limit());
        packet.limit(packet.capacity());
        logger.log(READ_TASK_POSITION, packet);
      }
    }
    //
    // Not sure how this occurs, it means we have read more than our limit and we
    // should log this and clear the buffer
    //
    else {
      packet.clear();
    }
  }

  public void read() throws IOException {
    int response = endPoint.readPacket(packet);
    logger.log(READ_TASK_COMPLETED, packet.position(), packet.limit(), response);
    if (response > 0) {
      handleDataToProcess(response);
    } else if (response < 0) {
      logger.log(READ_TASK_NEGATIVE_CLOSE);
      closeProtocol();
    } else {
      logger.log(READ_TASK_ZERO_BYTE, packet.position(), packet.limit(), packet.capacity());
      if (packet.position() == packet.limit()) {
        packet.clear();
      }
    }
  }

  class ScheduledRead implements Runnable {

    private final Selectable selectable;


    ScheduledRead(Selectable selectable) {
      this.selectable = selectable;
    }

    public void run() {
      try {
        endPoint.register(SelectionKey.OP_READ, selectable);
      } catch (IOException e) {
        logger.log(READ_TASK_EXCEPTION, e);
      }
    }
  }
}
