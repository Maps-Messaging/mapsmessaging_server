/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.utilities.stats;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import io.mapsmessaging.utilities.stats.processors.AdderDataProcessor;
import io.mapsmessaging.utilities.stats.processors.AverageDataProcessor;
import io.mapsmessaging.utilities.stats.processors.DifferenceDataProcessor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MovingAverageFactoryTest {

  @Test
  void getInstance() {
    assertNotNull(MovingAverageFactory.getInstance());
  }

  @Test
  void close() {
    LinkedMovingAverages linked = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Test", 1, 10, 6,  TimeUnit.SECONDS, "Tests");
    assertNotNull(linked);
    MovingAverageFactory.getInstance().close(linked);
    for(LinkedMovingAverages movingAverage:MovingAverageFactory.getInstance().movingAverages){
      assertNotEquals(linked, movingAverage);
    }
  }

  @Test
  void createLinked() {
    LinkedMovingAverages linked = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Test", 1, 10, 6,  TimeUnit.SECONDS, "Tests");
    assertNotNull(linked);
    MovingAverageFactory.getInstance().close(linked);
  }

  @Test
  void testCreateLinked() {
    int[] entries = {1, 2, 4, 8, 16, 32, 64};
    LinkedMovingAverages linked = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Test", entries,  TimeUnit.SECONDS, "Tests");
    assertNotNull(linked);
    assertTrue(linked.dataProcessor instanceof AdderDataProcessor);
    MovingAverageFactory.getInstance().close(linked);

    linked = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.AVE, "Test", entries,  TimeUnit.SECONDS, "Tests");
    assertNotNull(linked);
    assertTrue(linked.dataProcessor instanceof AverageDataProcessor);
    MovingAverageFactory.getInstance().close(linked);

    linked = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.DIFF, "Test", entries,  TimeUnit.SECONDS, "Tests");
    assertNotNull(linked);
    assertTrue(linked.dataProcessor instanceof DifferenceDataProcessor);
    MovingAverageFactory.getInstance().close(linked);
  }

  @Test
  void accumulator(){
    assertEquals("Average", ACCUMULATOR.AVE.getName());
    assertEquals("Adder", ACCUMULATOR.ADD.getName());
    assertEquals("Difference", ACCUMULATOR.DIFF.getName());
    assertNotNull(ACCUMULATOR.AVE.getDescription());
    assertNotNull(ACCUMULATOR.ADD.getDescription());
    assertNotNull(ACCUMULATOR.DIFF.getDescription());
  }
}