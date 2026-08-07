/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.engine.closure;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class ClosureTaskManagerTest {

  @Test
  void close_runsTasksInRegistrationOrder() {
    ClosureTaskManager manager = new ClosureTaskManager();
    List<String> executionOrder = new ArrayList<>();

    manager.add(() -> executionOrder.add("first"));
    manager.add(() -> executionOrder.add("second"));
    manager.add(() -> executionOrder.add("third"));

    manager.close();

    Assertions.assertEquals(List.of("first", "second", "third"), executionOrder);
  }

  @Test
  void remove_registeredTask_preventsExecution() {
    ClosureTaskManager manager = new ClosureTaskManager();
    AtomicInteger executionCount = new AtomicInteger();
    ClosureTask removedTask = executionCount::incrementAndGet;

    Assertions.assertTrue(manager.add(removedTask));
    Assertions.assertTrue(manager.remove(removedTask));
    Assertions.assertFalse(manager.remove(removedTask));

    manager.close();

    Assertions.assertEquals(0, executionCount.get());
  }

  @Test
  void add_sameTaskTwice_executesTaskTwice() {
    ClosureTaskManager manager = new ClosureTaskManager();
    AtomicInteger executionCount = new AtomicInteger();
    ClosureTask task = executionCount::incrementAndGet;

    Assertions.assertTrue(manager.add(task));
    Assertions.assertTrue(manager.add(task));

    manager.close();

    Assertions.assertEquals(2, executionCount.get());
  }

  @Test
  void close_clearsTasks_andManagerCanBeReused() {
    ClosureTaskManager manager = new ClosureTaskManager();
    AtomicInteger executionCount = new AtomicInteger();

    manager.add(executionCount::incrementAndGet);
    manager.close();
    manager.close();

    Assertions.assertEquals(1, executionCount.get());

    manager.add(executionCount::incrementAndGet);
    manager.close();

    Assertions.assertEquals(2, executionCount.get());
  }

  @Test
  void close_allowsTaskToRemoveItself() {
    ClosureTaskManager manager = new ClosureTaskManager();
    AtomicInteger executionCount = new AtomicInteger();
    ClosureTask[] selfRemovingTask = new ClosureTask[1];
    selfRemovingTask[0] = () -> {
      executionCount.incrementAndGet();
      manager.remove(selfRemovingTask[0]);
    };
    manager.add(selfRemovingTask[0]);
    manager.add(executionCount::incrementAndGet);

    manager.close();

    Assertions.assertEquals(2, executionCount.get());
  }
}
