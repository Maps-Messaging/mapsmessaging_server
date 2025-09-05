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
import io.nats.client.*;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsPubSubTest extends BaseTestConfig {

  private Connection connection;

  @BeforeEach
  void setup() throws Exception {
    Options options = new Options.Builder()
        .server("nats://localhost:4222")
        .connectionTimeout(Duration.ofSeconds(10))
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
  void testBasicPubSub() throws Exception {
    String subject = "pubsub.basic";
    CompletableFuture<String> future = new CompletableFuture<>();

    Dispatcher dispatcher = connection.createDispatcher(msg -> {
      future.complete(new String(msg.getData()));
    });
    dispatcher.subscribe(subject);

    connection.publish(subject, "Basic Test".getBytes());
    connection.flush(Duration.ofSeconds(2));

    String received = future.get(3, TimeUnit.SECONDS);
    assertEquals("Basic Test", received);
  }

  @Test
  @Order(2)
  void testWildcardPubSub() throws Exception {
    String subject = "pubsub.test.foo";
    CompletableFuture<String> future = new CompletableFuture<>();

    Dispatcher dispatcher = connection.createDispatcher(msg -> {
      future.complete(msg.getSubject());
    });
    dispatcher.subscribe("pubsub.test.*");

    connection.publish(subject, "match".getBytes());
    connection.flush(Duration.ofSeconds(2));

    String receivedSubject = future.get(3, TimeUnit.SECONDS);
    assertEquals(subject, receivedSubject);
  }

  @Test
  @Order(3)
  void testQueueGroupDelivery() throws Exception {
    String subject = "pubsub.queue";
    CompletableFuture<String> future1 = new CompletableFuture<>();
    CompletableFuture<String> future2 = new CompletableFuture<>();

    Dispatcher d1 = connection.createDispatcher(msg -> future1.complete("worker1"));
    Dispatcher d2 = connection.createDispatcher(msg -> future2.complete("worker2"));

    d1.subscribe(subject, "group1");
    d2.subscribe(subject, "group1");

    connection.publish(subject, "Queued".getBytes());
    connection.flush(Duration.ofSeconds(2));

    String winner = CompletableFuture.anyOf(future1, future2).get(3, TimeUnit.SECONDS).toString();
    assertTrue(winner.equals("worker1") || winner.equals("worker2"));
  }

  @Test
  @Order(4)
  void testUnsubDoesNotReceive() throws Exception {
    String subject = "pubsub.unsub";
    CompletableFuture<String> future = new CompletableFuture<>();

    Dispatcher dispatcher = connection.createDispatcher(msg -> future.complete("FAIL"));
    dispatcher.subscribe(subject);
    dispatcher.unsubscribe(subject);

    connection.publish(subject, "No listener".getBytes());
    connection.flush(Duration.ofSeconds(2));

    assertThrows(Exception.class, () -> future.get(1, TimeUnit.SECONDS));
  }
}
