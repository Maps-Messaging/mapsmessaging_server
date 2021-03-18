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

import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;

/**
 * This class implements a ConcurrentTaskScheduler with a priority based concurrent queue. This enables tasks with a higher priority to
 * take precedence over tasks with a lower priority.
 *
 * @param <V> - is what will be returned by the future on completion of the task
 *
 * @since 1.0
 * @author Matthew Buckton
 * @version 1.0
 */
@ToString
public class PriorityConcurrentTaskScheduler<V> extends ConcurrentTaskScheduler<V> implements PriorityTaskScheduler<V> {

  private final List<Queue<FutureTask<V>>> queues;

  /**
   * Constructs the concurrent priority queue, specifying the depth of the priority and the unique domain name that this task queue manages
   *
   * @param domain  a unique domain name
   * @param prioritySize the number of unique priority levels
   */
  public PriorityConcurrentTaskScheduler(@NonNull @NotNull String domain, int prioritySize) {
    super(domain);
    queues = new ArrayList<>();
    for(int x=0;x<prioritySize;x++){
      queues.add(new ConcurrentLinkedQueue<>());
    }
  }

  @Override
  public void addTask(@NonNull @NotNull FutureTask<V> task) {
    addTask(task, 0);
  }

  @Override
  public void addTask(@NonNull @NotNull FutureTask<V> task, int priority) {
    if(!shutdown) {
      queues.get(priority).add(task);
      executeQueue();
    }
    else{
      task.cancel(true);
    }
  }

  @Override
  public boolean isEmpty(){
    for(Queue<FutureTask<V>> queue:queues){
      if(!queue.isEmpty()){
        return false;
      }
    }
    return true;
  }

  @Override
  protected @Nullable FutureTask<V> poll(){
    for(Queue<FutureTask<V>> queue:queues){
      FutureTask<V> task = queue.poll();
      if(task != null){
        return task;
      }
    }
    return null;
  }
}


