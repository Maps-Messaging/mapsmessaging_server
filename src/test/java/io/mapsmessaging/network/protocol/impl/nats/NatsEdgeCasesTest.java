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

package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.test.BaseTestConfig;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsEdgeCasesTest extends BaseTestConfig {

  private Connection connection;

  @BeforeEach
  void setup() throws Exception {
    Options options = new Options.Builder()
        .server("nats://localhost:4222")
        .connectionTimeout(Duration.ofSeconds(5))
        .build();
    connection = Nats.connect(options);
  }

  @AfterEach
  void teardown() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  @Order(1)
  void testInvalidSubject() {
    assertThrows(IllegalArgumentException.class, () -> {
      connection.publish("invalid subject", "bad".getBytes());
    });
  }

  @Test
  @Order(2)
  void testOversizedPayload() {
    byte[] largePayload = new byte[1024 * 1024 * 10]; // 10MB (likely exceeds server config)
    assertThrows(IllegalArgumentException.class, () -> {
      connection.publish("test.large", largePayload);
      connection.flush(Duration.ofSeconds(5));
    });
  }

  @Test
  @Order(3)
  void testMalformedMessage() {
    // Simulate broken stream manually
    assertDoesNotThrow(() -> {
      // This won't throw directly here, but your server should reject it internally
      connection.publish("test.malformed", "MSG without length".getBytes());
      connection.flush(Duration.ofSeconds(1));
    });
  }

  @Test
  @Order(4)
  void testDuplicateSubscriptionIds() throws Exception {
    String subject = "edge.dup.sid";
    CompletableFuture<String> future = new CompletableFuture<>();

    Dispatcher d1 = connection.createDispatcher(msg -> future.complete("first"));
    d1.subscribe(subject);

    // Duplicate subscription to same subject using different Dispatcher
    Dispatcher d2 = connection.createDispatcher(msg -> future.complete("second"));
    d2.subscribe(subject);

    connection.publish(subject, "check".getBytes());
    connection.flush(Duration.ofSeconds(2));

    String who = future.get(2, TimeUnit.SECONDS);
    assertTrue(who.equals("first") || who.equals("second"));
  }

  @Test
  @Order(5)
  void testMultipleMessagesInSinglePacket() throws Exception {
    String subject = "edge.batch";
    CompletableFuture<Integer> counter = new CompletableFuture<>();
    int[] count = {0};

    Dispatcher dispatcher = connection.createDispatcher(msg -> {
      count[0]++;
      if (count[0] == 3) counter.complete(count[0]);
    });
    dispatcher.subscribe(subject);

    for (int i = 0; i < 3; i++) {
      connection.publish(subject, ("msg" + i).getBytes());
    }
    connection.flush(Duration.ofSeconds(20));

    Integer received = counter.get(3, TimeUnit.SECONDS);
    assertEquals(3, received);
  }
}
