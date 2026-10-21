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

package io.mapsmessaging.tools.config.schema.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonInstanceGeneratorNumericTest {
  @Test
  void generated_numbers_stay_within_declared_bounds() throws Exception {
    JsonNode schema = new ObjectMapper().readTree("{\"type\":\"number\",\"minimum\":0.25,\"maximum\":0.75}");
    JsonInstanceGenerator generator = new JsonInstanceGenerator(356);
    for (int sample = 0; sample < 100; sample++) {
      double value = generator.generateValidInstance(schema).asDouble();
      assertTrue(value >= 0.25 && value <= 0.75, "Out-of-range value: " + value);
    }
  }

  @Test
  void equal_numeric_bounds_produce_that_value() throws Exception {
    JsonNode schema = new ObjectMapper().readTree("{\"type\":\"number\",\"minimum\":0.5,\"maximum\":0.5}");
    assertEquals(0.5, new JsonInstanceGenerator(356).generateValidInstance(schema).asDouble());
  }
}
