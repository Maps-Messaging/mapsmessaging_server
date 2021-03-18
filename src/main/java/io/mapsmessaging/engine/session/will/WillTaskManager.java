/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.session.will;

import java.util.LinkedHashMap;
import java.util.Map;

public class WillTaskManager {

  private static final WillTaskManager instance = new WillTaskManager();
  /**
   * Static storage
   */
  private Map<String, WillDetails> willTaskMap;
  /**
   * Dynamic operation
   */
  private final Map<String, WillTaskImpl> willActiveTasks;

  public WillTaskManager() {
    willActiveTasks = new LinkedHashMap<>();
  }

  public static WillTaskManager getInstance() {
    return instance;
  }

  public void setMap(Map<String, WillDetails> willTaskMap) {
    this.willTaskMap = willTaskMap;
  }

  public WillTaskImpl remove(String id) {
    willTaskMap.remove(id);
    return willActiveTasks.remove(id);
  }

  public WillTaskImpl put(String id, WillDetails willDetails) {
    WillTaskImpl task = new WillTaskImpl(willDetails);
    willTaskMap.put(id, willDetails);
    willActiveTasks.put(id, task);
    return task;
  }

  public WillTaskImpl get(String id) {
    return willActiveTasks.get(id);
  }

  public void start() {
    for (WillDetails willDetails : willTaskMap.values()) {
      WillTaskImpl task = new WillTaskImpl(willDetails);
      task.schedule();
    }
  }

  public void stop() {
    // Nothing to stop here
  }

}