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

package org.maps.network.io.impl;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.io.EndPoint;
import org.maps.network.io.Selectable;
import org.maps.network.io.ServerPacket;
import org.maps.utilities.configuration.ConfigurationProperties;

public class SelectorTask implements Selectable {

  private final Logger logger;
  private final EndPoint endPoint;
  private final WriteTask writeTask;
  private final ReadTask readTask;

  private int selectionOps;
  private FutureTask<SelectionKey> future;
  private SelectionKey selectionKey;
  private boolean isOpen;

  public SelectorTask(SelectorCallback selectorCallback, ConfigurationProperties properties) {
    this(selectorCallback, properties, false);
  }

  public SelectorTask(SelectorCallback selectorCallback, ConfigurationProperties properties, boolean isUDP) {
    logger = LoggerFactory.getLogger(SelectorTask.class);
    int readBufferSize = properties.getIntProperty("serverReadBufferSize", DefaultConstants.TCP_READ_BUFFER_SIZE);
    int writeBufferSize = properties.getIntProperty("serverWriteBufferSize", DefaultConstants.TCP_WRITE_BUFFER_SIZE);

    if (isUDP) {
      readTask = new UDPReadTask(selectorCallback, readBufferSize, logger);
      writeTask = new UDPWriteTask(selectorCallback, writeBufferSize, this, logger);
    } else {
      int readDelay = -1;
      int readFragmentation = -1;
      boolean readDelayEnabled = properties.getBooleanProperty("enableReadDelayOnFragmentation", DefaultConstants.TCP_READ_DELAY_ENABLED);
      if(readDelayEnabled){
        readDelay = properties.getIntProperty("readDelayOnFragmentation", DefaultConstants.TCP_READ_DELAY_ON_FRAGMENTATION);
        if(readDelay <= 0){
          readDelay = DefaultConstants.TCP_READ_DELAY_ON_FRAGMENTATION;
        }
        readFragmentation = properties.getIntProperty("enableReadDelayOnFragmentation", DefaultConstants.TCP_READ_FRAGMENTATION_LIMIT);
      }
      readTask = new ReadTask(selectorCallback, readBufferSize, logger, readDelay, readFragmentation);
      writeTask = new WriteTask(selectorCallback, writeBufferSize, this, logger);
    }
    this.endPoint = selectorCallback.getEndPoint();
    isOpen = true;
    selectionOps = 0;
    logger.log(LogMessages.SELECTOR_NEW_TASK);
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

  public synchronized void close() {
    isOpen = false;
    selectionKey.cancel();
    logger.log(LogMessages.SELECTOR_CLOSE_TASK);
  }

  public void push(ServerPacket frame) {
    writeTask.push(frame);
    if(logger.isInfoEnabled()) {
      logger.log(LogMessages.SELECTOR_PUSH_WRITE, frame.getClass(), writeTask.size());
    }
  }

  public synchronized void register(int selection) throws IOException {
    if (isOpen) {
      logger.log(LogMessages.SELECTOR_REGISTERING, selectorOpToString(selection));
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
          if (cause instanceof IOException) {
            throw (IOException) cause;
          }
          Thread.currentThread().interrupt();
        }
        logger.log(LogMessages.SELECTOR_REGISTER_RESULT, selectorOpToString(selectionKey.interestOps()));
      }
    } else {
      logger.log(LogMessages.SELECTOR_REGISTER_CLOSED_TASK, selectorOpToString(selection));
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
      logger.log(LogMessages.SELECTOR_CANCELLING, selectorOpToString(selection), selectorOpToString(selectionOps));
      future = endPoint.register(selectionOps, this);
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    if (isOpen) {
      logger.log(LogMessages.SELECTOR_CALLED_BACK, selectorOpToString(selection));
      if ((selection & OP_READ) != 0) {
        logger.log(LogMessages.SELECTOR_READ_TASK);
        readTask.selected(selectable, selector, OP_READ);
      }
      if ((selection & OP_WRITE) != 0) {
        logger.log(LogMessages.SELECTOR_WRITE_TASK);
        writeTask.selected(selectable,selector, OP_WRITE);
      }
    }
  }

  public ReadTask getReadTask() {
    return readTask;
  }
}
