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

import io.mapsmessaging.config.network.impl.TcpConfig;
import io.mapsmessaging.config.network.impl.UdpConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.TcpConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.ServerPacket;
import lombok.Getter;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class SelectorTask implements Selectable {

  @Getter
  private final ReadTask readTask;

  private final Logger logger;
  private final EndPoint endPoint;
  private final WriteTask writeTask;

  private int selectionOps;
  private FutureTask<SelectionKey> future;
  private SelectionKey selectionKey;
  private boolean isOpen;

  public SelectorTask(SelectorCallback selectorCallback, EndPointConfigDTO properties) {
    this(selectorCallback, properties, false);
  }

  public SelectorTask(SelectorCallback selectorCallback, EndPointConfigDTO properties, boolean isUDP) {
    logger = LoggerFactory.getLogger(SelectorTask.class);
    int readBufferSize = (int)properties.getServerReadBufferSize();
    int writeBufferSize = (int) properties.getServerWriteBufferSize();
    if (isUDP) {
      long packetThreshold = 1000;
      if(properties instanceof UdpConfig udpConfig){
        packetThreshold = udpConfig.getPacketReuseTimeout();
      }
      readTask = new UDPReadTask(selectorCallback, readBufferSize, packetThreshold, logger);
      writeTask = new UDPWriteTask(selectorCallback, writeBufferSize, this, logger);
    } else {
      int readDelay = -1;
      int readFragmentation = -1;
      if (properties instanceof TcpConfig) {
        boolean readDelayEnabled = ((TcpConfigDTO) properties).isEnableReadDelayOnFragmentation();
        if (readDelayEnabled) {
          readDelay = ((TcpConfigDTO) properties).getReadDelayOnFragmentation();
          if (readDelay <= 0) {
            readDelay = DefaultConstants.TCP_READ_DELAY_ON_FRAGMENTATION;
          }
          readFragmentation = ((TcpConfigDTO) properties).getFragmentationLimit();
        }
      }
      readTask = new ReadTask(selectorCallback, readBufferSize, logger, readDelay, readFragmentation);
      writeTask = new WriteTask(selectorCallback, writeBufferSize, this, logger);
    }
    this.endPoint = selectorCallback.getEndPoint();
    isOpen = true;
    selectionOps = 0;
    logger.log(ServerLogMessages.SELECTOR_NEW_TASK);
  }

  public synchronized void close() {
    isOpen = false;
    selectionKey.cancel();
    logger.log(ServerLogMessages.SELECTOR_CLOSE_TASK);
  }

  public void push(ServerPacket frame) {
    writeTask.push(frame);
    if (logger.isInfoEnabled()) {
      logger.log(ServerLogMessages.SELECTOR_PUSH_WRITE, frame.getClass(), writeTask.size());
    }
  }

  public synchronized void register(int selection) throws IOException {
    if (isOpen) {
      logger.log(ServerLogMessages.SELECTOR_REGISTERING, selectorOpToString(selection));
      selectionOps = selection | selectionOps;
      future = endPoint.register(selectionOps, this);
      if (future != null && future.isDone()) {
        try {
          selectionKey = future.get();
          future = null;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof IOException ioException) {
            throw ioException;
          }
          Thread.currentThread().interrupt();
        }
        logger.log(ServerLogMessages.SELECTOR_REGISTER_RESULT, selectorOpToString(selectionKey.interestOps()));
      }
    } else {
      logger.log(ServerLogMessages.SELECTOR_REGISTER_CLOSED_TASK, selectorOpToString(selection));
    }
  }

  public synchronized void cancel(int selection) throws IOException {
    selectionOps = selectionOps & ~selection;
    if (future != null) {
      try {
        selectionKey = future.get(100, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (ExecutionException e) {
        throw new IOException(e);
      } catch (TimeoutException e) {
        // Nothing to do yet.. ToDo: work on the futures
      }
    }
    if (selectionKey != null && isOpen) {
      logger.log(ServerLogMessages.SELECTOR_CANCELLING, selectorOpToString(selection), selectorOpToString(selectionOps));
      future = endPoint.register(selectionOps, this);
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    if (isOpen) {
      logger.log(ServerLogMessages.SELECTOR_CALLED_BACK, selectorOpToString(selection));
      if ((selection & OP_READ) != 0) {
        logger.log(ServerLogMessages.SELECTOR_READ_TASK);
        readTask.selected(selectable, selector, OP_READ);
      }
      if ((selection & OP_WRITE) != 0) {
        logger.log(ServerLogMessages.SELECTOR_WRITE_TASK);
        writeTask.selected(selectable, selector, OP_WRITE);
      }
    }
  }


  private static String selectorOpToString(int op) {
    StringBuilder sb = new StringBuilder();
    if (op == 0) {
      sb.append("NONE");
    } else {
      if ((op & 0x1) != 0) {
        sb.append("READ ");
      }
      if ((op & 0x4) != 0) {
        sb.append("WRITE ");
      }
      if ((op & 0x8) != 0) {
        sb.append("CONNECT ");
      }
      if ((op & 0x10) != 0) {
        sb.append("ACCEPT ");
      }
    }
    return sb.toString().trim();
  }

}
