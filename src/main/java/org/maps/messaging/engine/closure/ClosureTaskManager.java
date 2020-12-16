/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.engine.closure;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public class ClosureTaskManager implements Closeable {

  private final List<ClosureTask> closureTasks;

  public ClosureTaskManager(){
    closureTasks = new ArrayList<>();
  }

  public synchronized boolean add(ClosureTask closureTask){
    return closureTasks.add(closureTask);
  }

  public synchronized boolean remove(ClosureTask closureTask){
    return closureTasks.remove(closureTask);
  }

  public void close(){
   for(ClosureTask task:closureTasks){
      task.run();
    }
    closureTasks.clear();
  }
}
