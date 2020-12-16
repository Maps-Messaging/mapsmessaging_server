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

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;

public class SelectorOld extends Selector {

  private final Queue<FutureTask<SelectionKey>> registryQueue;
  private Thread selectorThread;

  public SelectorOld() throws IOException {
    super();
    registryQueue = new ConcurrentLinkedQueue<>();
  }

  @Override
  public void run() {
    selectorThread = Thread.currentThread();
    super.run();
  }

  @Override
  public FutureTask<SelectionKey> register(
      SelectableChannel selectable, int key, Object attachment) {
    if (Thread.currentThread() != selectorThread) {
      final RegisterCallable details = new RegisterCallable(selectable, key, attachment);
      FutureTask<SelectionKey> task = new FutureTask<>(details);
      registryQueue.add(task);
      channelSelector.wakeup();
      return task;
    } else {
      return super.register(selectable, key, attachment);
    }
  }

  @Override
  protected void processRegistryQueue() {
    while (!registryQueue.isEmpty()) {
      FutureTask<SelectionKey> task = registryQueue.remove();
      task.run();
    }
  }
}
