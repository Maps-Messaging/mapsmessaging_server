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

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;

public class SelectorLoadManager {

  private final Logger logger;
  private final Selector[] selectors;
  private final Executor selectorExecutor;
  private int index;

  public SelectorLoadManager(int poolSize) throws IOException {
    logger = LoggerFactory.getLogger(SelectorLoadManager.class.getName());
    selectorExecutor = Executors.newFixedThreadPool(poolSize);
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
    logger.log(LogMessages.END_POINT_MANAGER_NEW_SELECTOR);
    selectorExecutor.execute(selector);
    return selector;
  }


}
