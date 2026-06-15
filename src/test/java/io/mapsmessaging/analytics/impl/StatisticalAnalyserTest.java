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

package io.mapsmessaging.analytics.impl;

import io.mapsmessaging.analytics.Analyser;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatisticalAnalyserTest {

  @Test
  void create_returnsConfiguredStatisticalAnalyserWithoutReusingPrototype() {
    StatisticalAnalyser prototype = new StatisticalAnalyser();
    StatisticsConfigDTO config = new StatisticsConfigDTO("Advanced", 25, List.of("serial"), List.of("temperature"));

    Analyser created = prototype.create(config);

    assertInstanceOf(StatisticalAnalyser.class, created);
    assertNotSame(prototype, created);
    assertEquals("stats", created.getName());
    assertEquals("Statistical Event Analyser", created.getDescription());
  }

  @Test
  void flush_withoutIngestedEntries_returnsEmptyJsonMessage() {
    StatisticalAnalyser analyser = new StatisticalAnalyser("Advanced", 10, List.of(), List.of());

    Message result = analyser.flush();

    assertEquals("application/json", result.getContentType());
    assertEquals("{}", new String(result.getOpaqueData(), StandardCharsets.UTF_8));
  }

  @Test
  void close_hasNoResourcesAndDoesNotThrow() {
    StatisticalAnalyser analyser = new StatisticalAnalyser();

    assertDoesNotThrow(analyser::close);
  }
}
