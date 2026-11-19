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

package io.mapsmessaging.aggregator.aggregate;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AggregationEngineTest {

  @Test
  void aggregate_registeredStrategy_receivesInputsAndReturnsResult() {
    AggregationEngine engine = new AggregationEngine();
    Message expected = new MessageBuilder().build();
    String[] topics = {"/input/one"};
    Message[] contributions = {new MessageBuilder().build()};

    engine.register(new AggregationStrategy() {
      @Override
      public String getName() {
        return "test";
      }

      @Override
      public Message aggregate(String[] receivedTopics, Message[] receivedContributions) {
        assertSame(topics, receivedTopics);
        assertSame(contributions, receivedContributions);
        return expected;
      }
    });

    assertSame(expected, engine.aggregate("test", topics, contributions));
  }

  @Test
  void register_sameName_replacesPreviousStrategy() {
    AggregationEngine engine = new AggregationEngine();
    Message first = new MessageBuilder().build();
    Message replacement = new MessageBuilder().build();

    engine.register(strategyReturning("duplicate", first));
    engine.register(strategyReturning("duplicate", replacement));

    assertSame(replacement, engine.aggregate("duplicate", new String[0], new Message[0]));
  }

  @Test
  void aggregate_unknownStrategy_throwsUsefulException() {
    AggregationEngine engine = new AggregationEngine();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> engine.aggregate("missing", new String[0], new Message[0])
    );

    assertEquals("Unknown aggregation strategy: missing", exception.getMessage());
  }

  private AggregationStrategy strategyReturning(String name, Message result) {
    return new AggregationStrategy() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Message aggregate(String[] topics, Message[] contributions) {
        return result;
      }
    };
  }
}
