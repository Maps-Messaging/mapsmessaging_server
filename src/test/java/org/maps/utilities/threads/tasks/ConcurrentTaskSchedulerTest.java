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

package org.maps.utilities.threads.tasks;

import org.junit.jupiter.api.Test;
import org.maps.test.WaitForState;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

abstract class ConcurrentTaskSchedulerTest {

  protected abstract ConcurrentTaskScheduler<Object> create();

  @Test
  void shutdown() {
    ConcurrentTaskScheduler<Object> taskScheduler = create();
    taskScheduler.shutdown(true);
    FutureTask<Object> futureTask = new FutureTask<>(new Task());
    taskScheduler.addTask(futureTask);
    assertTrue(futureTask.isCancelled());
  }

  @Test
  void getters() {
    ConcurrentTaskScheduler<Object> taskScheduler = create();
    FutureTask<Object> futureTask = new FutureTask<>(new Task());
    taskScheduler.addTask(futureTask);
    assertTrue(futureTask.isDone());
    assertEquals(1, taskScheduler.getTotalTasksQueued());
    assertEquals(1, taskScheduler.getMaxOutstanding());
    assertEquals(0, taskScheduler.getOffloadCount());
    assertEquals(0, taskScheduler.getOutstanding());
  }

  @Test
  void addTask() throws IOException {
    ConcurrentTaskScheduler<Object> taskScheduler = create();
    assertNotNull(taskScheduler);
    assertTrue(taskScheduler.isEmpty());
    FutureTask<Object> futureTask = new FutureTask<>(new Task());
    taskScheduler.addTask(futureTask);
    WaitForState.waitFor(100, TimeUnit.MILLISECONDS, futureTask::isDone);
    assertTrue(taskScheduler.isEmpty());
  }

  @Test
  void testToString() {
  }

  private class Task implements Callable<Object>{

    @Override
    public Object call() throws Exception {
      return this;
    }
  }
}