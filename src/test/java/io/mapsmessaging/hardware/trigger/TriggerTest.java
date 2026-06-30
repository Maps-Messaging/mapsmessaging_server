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

package io.mapsmessaging.hardware.trigger;

import io.mapsmessaging.dto.rest.config.device.triggers.BaseTriggerConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TriggerTest {

  @Test
  void runActions_runsRegisteredTasksInOrder() {
    TestTrigger trigger = new TestTrigger();
    List<String> calls = new ArrayList<>();
    trigger.addTask(() -> calls.add("first"));
    trigger.addTask(() -> calls.add("second"));

    trigger.fire();

    assertEquals(List.of("first", "second"), calls);
  }

  @Test
  void removeTask_preventsFutureExecution() {
    TestTrigger trigger = new TestTrigger();
    List<String> calls = new ArrayList<>();
    Runnable removed = () -> calls.add("removed");
    trigger.addTask(removed);
    trigger.addTask(() -> calls.add("retained"));

    trigger.removeTask(removed);
    trigger.fire();

    assertEquals(List.of("retained"), calls);
  }

  private static final class TestTrigger extends Trigger {

    private void fire() {
      runActions();
    }

    @Override
    public Trigger build(BaseTriggerConfigDTO properties) {
      return this;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
      return "test";
    }

    @Override
    public String getDescription() {
      return "Test trigger";
    }
  }
}
