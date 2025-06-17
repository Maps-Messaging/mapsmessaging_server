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
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.Selectable;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class Selector implements SelectorInt {

  protected java.nio.channels.Selector channelSelector;
  private final Logger logger;
  private final AtomicBoolean isOpen;

  public Selector() throws IOException {
    logger = LoggerFactory.getLogger(Selector.class);
    logger.log(ServerLogMessages.SELECTOR_OPEN);
    channelSelector = java.nio.channels.Selector.open();
    isOpen = new AtomicBoolean(true);
  }

  @Override
  public void run() {
    Thread.currentThread().setName("SelectorThread");
    int emptySelectCount = 0;
    final int SPIN_THRESHOLD = 1000;
    while (isOpen.get()) {
      try {
        int selected = channelSelector.select();
        //
        // Process any of the fired keys and process the attachments
        //
        if (selected == 0) {
          emptySelectCount++;
          if (emptySelectCount >= SPIN_THRESHOLD) {
            logger.log(ServerLogMessages.SELECTOR_SPIN_DETECTED, SPIN_THRESHOLD);
            emptySelectCount = 0;
            rebuildSelector();
          } else {
            Thread.yield();
          }
          continue;
        }

        emptySelectCount = 0;
        if (selected > 0) {
          Set<SelectionKey> selectedKeys = channelSelector.selectedKeys();
          processSelectionList(selectedKeys);
        }

        //
        // Before we enter the select again, lets add any waiting requests
        //
        processRegistryQueue();
      } catch (IOException e) {
        logger.log(ServerLogMessages.SELECTOR_FAILED_ON_CALL);
        isOpen.set(false);
      }
    }
  }

  private void rebuildSelector() {
    try {
      java.nio.channels.Selector newSelector = java.nio.channels.Selector.open();

      for (SelectionKey key : channelSelector.keys()) {
        try {
          if (key.isValid()) {
            key.channel().register(newSelector, key.interestOps(), key.attachment());
          }
        } catch (CancelledKeyException ignored) {
          // Key may already be cancelled
        }
      }

      channelSelector.close();
      channelSelector = newSelector;
      logger.log(ServerLogMessages.SELECTOR_REBUILT);
    } catch (Exception e) {
      logger.log(ServerLogMessages.SELECTOR_REBUILD_FAILED, e.getMessage());
      isOpen.set(false);
    }
  }

  private void processSelectionList(Set<SelectionKey> selectedKeys) {
    Iterator<SelectionKey> iter = selectedKeys.iterator();
    while (iter.hasNext()) {
      SelectionKey key = iter.next();
      try {
        if (key.attachment() instanceof Selectable selectable) {
          if (logger.isDebugEnabled()) {
            logger.log(ServerLogMessages.SELECTOR_FIRED, key.interestOps());
          }
          selectable.selected(selectable, this, key.interestOps());
        }
      } catch (CancelledKeyException cancelled) {
        logger.log(ServerLogMessages.SELECTOR_CONNECTION_CLOSE);
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.log(ServerLogMessages.SELECTOR_TASK_FAILED, e, key.toString());
        } else {
          logger.log(ServerLogMessages.SELECTOR_TASK_FAILED_1, key.toString());
        }
      } finally {
        iter.remove();
      }
    }
  }

  protected void processRegistryQueue() {
    // This implementation is thread safe after JDK 11, before hand it was not
  }

  @Override
  public void close() {
    isOpen.set(false);
  }

  public FutureTask<SelectionKey> register(SelectableChannel selectable, int key, Object attachment) {
    FutureTask<SelectionKey> task = new FutureTask<>(new RegisterCallable(selectable, key, attachment));
    task.run();
    channelSelector.wakeup();
    return task;
  }

  public void wakeup() {
    channelSelector.wakeup();
  }

  protected final class RegisterCallable implements Callable<SelectionKey> {

    private final SelectableChannel selectable;
    private final int key;
    private final Object attachment;

    public RegisterCallable(SelectableChannel selectable, int key, Object attachment) {
      this.selectable = selectable;
      this.key = key;
      this.attachment = attachment;
    }

    @Override
    public SelectionKey call() throws ClosedChannelException {
      return selectable.register(channelSelector, key, attachment);
    }
  }
}
