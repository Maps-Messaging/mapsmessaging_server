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

package io.mapsmessaging.utilities.threads.tasks;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class implements a single concurrent task queue
 *
 * @param <V> - is what will be returned by the future on completion of the task
 *
 *  @since 1.0
 *  @author Matthew Buckton
 *  @version 1.0
 */
@ToString
public class SingleConcurrentTaskScheduler<V> extends ConcurrentTaskScheduler<V> {

  private final Queue<FutureTask<V>> queue;

  /**
   * Constructs a single queue based task queue
   *
   * @param domain a unique name defining the domain this task queue manages
   */
  public SingleConcurrentTaskScheduler(@NonNull @NotNull String domain) {
    super(domain);
    queue = new ConcurrentLinkedQueue<>();
  }

  @Override
  public void addTask(@NonNull @NotNull FutureTask<V> task) {
    if(!shutdown) {
      queue.add(task);
      executeQueue();
    }
    else{
      task.cancel(true); // Mark it as cancelled
    }
  }

  @Override
  public boolean isEmpty(){
    return queue.isEmpty();
  }

  @Override
  protected @Nullable FutureTask<V> poll(){
    return queue.poll();
  }
}


