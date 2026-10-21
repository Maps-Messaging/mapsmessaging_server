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

package io.mapsmessaging.analytics.impl.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsFactoryTest {

  @Test
  void getInstance_returnsSingleton() {
    assertSame(StatisticsFactory.getInstance(), StatisticsFactory.getInstance());
  }

  @Test
  void getAnalyser_knownName_returnsFreshMatchingInstances() {
    Statistics first = StatisticsFactory.getInstance().getAnalyser("Advanced");
    Statistics second = StatisticsFactory.getInstance().getAnalyser("Advanced");

    assertInstanceOf(AdvancedStatistics.class, first);
    assertInstanceOf(AdvancedStatistics.class, second);
    assertNotSame(first, second);
  }

  @Test
  void getAnalyser_unknownName_fallsBackToBaseStatistics() {
    Statistics result = StatisticsFactory.getInstance().getAnalyser("not-registered");

    assertInstanceOf(BaseStatistics.class, result);
    assertEquals("Base", result.getName());
  }
}
