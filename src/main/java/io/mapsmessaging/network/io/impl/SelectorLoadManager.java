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

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class SelectorLoadManager {

  private final Logger logger;
  private final Selector[] selectors;
  private final Executor selectorExecutor;
  private int index;

  public SelectorLoadManager(int poolSize, String name) throws IOException {
    logger = LoggerFactory.getLogger(SelectorLoadManager.class.getName());
    selectorExecutor = createThreadPool(poolSize, name);
    selectors = new Selector[poolSize];
    for (int x = 0; x < poolSize; x++) {
      selectors[x] = create();
    }
    index = 0;
  }

  public synchronized void close() {
    for (Selector selector : selectors) {
      selector.close();
    }
  }

  public synchronized Selector allocate() {
    return selectors[(index++) % selectors.length];
  }

  private Selector create() throws IOException {
    Selector selector = new Selector();
    logger.log(ServerLogMessages.END_POINT_MANAGER_NEW_SELECTOR);
    selectorExecutor.execute(selector);
    return selector;
  }

  private Executor createThreadPool(int poolSize, String name){
    return new ThreadPoolExecutor(poolSize,
    poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
        new ThreadFactoryImpl(name)) ;
  }

  private static class ThreadFactoryImpl implements ThreadFactory{

    private final ThreadGroup threadGroup;

    public ThreadFactoryImpl(String name){
      threadGroup = new ThreadGroup(name);
    }

    @Override
    public Thread newThread(@NotNull Runnable r) {
      Thread t = new Thread(threadGroup, r);
      return t;
    }
  }
}
