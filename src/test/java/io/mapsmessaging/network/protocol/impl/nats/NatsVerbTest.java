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
import io.nats.client.impl.Headers;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsVerbsTest extends BaseTestConfig {

  private Connection connection;

  @BeforeEach
  void setup() throws Exception {
    Options options = new Options.Builder()
        .server("nats://localhost:4222")
        .connectionTimeout(Duration.ofSeconds(40))
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
  void testConnectAndInfo() {
    assertTrue(connection.getStatus() == Connection.Status.CONNECTED, "Should be connected");
  }

  @Test
  @Order(2)
  void testPubSub() throws Exception {
    String subject = "test.pubsub";
    String message = "Hello NATS";
    CompletableFuture<String> future = new CompletableFuture<>();

    Dispatcher dispatcher = connection.createDispatcher(msg -> {
      future.complete(new String(msg.getData()));
    });
    dispatcher.subscribe(subject);

    connection.publish(subject, message.getBytes());
    connection.flush(Duration.ofSeconds(2));

    String received = future.get(6, TimeUnit.SECONDS);
    assertEquals(message, received, "Received message should match published message");
  }

  @Test
  @Order(3)
  void testUnsub() throws Exception {
    String subject = "test.unsub";
    CompletableFuture<String> future = new CompletableFuture<>();

    Dispatcher dispatcher = connection.createDispatcher(msg -> {
      future.complete(new String(msg.getData()));
    });
    dispatcher.subscribe(subject);
    dispatcher.unsubscribe(subject);

    connection.publish(subject, "Should not be received".getBytes());
    connection.flush(Duration.ofSeconds(5));

    assertThrows(Exception.class, () -> future.get(1, TimeUnit.SECONDS));
  }

  @Test
  @Order(4)
  void testPingPong() throws Exception {
    // flush() sends PING and waits for PONG
    connection.flush(Duration.ofSeconds(2));
    assertTrue(connection.getStatus() == Connection.Status.CONNECTED, "Still connected after flush (PING/PONG)");
  }

  @Test
  @Order(5)
  void testErrorHandling() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> {
      connection.publish("", "bad".getBytes());
    });
  }

  @Test
  @Order(6)
  void testPubSubWithHeaders() throws Exception {
    String subject = "test.headers";
    String headerKey = "test-key";
    String headerValue = "test-value";
    String messageBody = "Hello with Headers";

    CompletableFuture<String> payloadFuture = new CompletableFuture<>();
    CompletableFuture<String> headerFuture = new CompletableFuture<>();

    Dispatcher dispatcher = connection.createDispatcher(msg -> {
      if (msg.hasHeaders()) {
        String val = msg.getHeaders().getFirst(headerKey);
        headerFuture.complete(val);
      } else {
        headerFuture.complete(null);
      }
      payloadFuture.complete(new String(msg.getData()));
    });
    dispatcher.subscribe(subject);

    // Manually create headers
    Headers headers = new Headers();
    headers.add(headerKey, headerValue);

    // Publish with headers
    connection.publish(subject, null, headers, messageBody.getBytes());
    connection.flush(Duration.ofSeconds(10));

    // Validate
    String receivedPayload = payloadFuture.get(6, TimeUnit.SECONDS);
    String receivedHeader = headerFuture.get(6, TimeUnit.SECONDS);

    assertEquals(messageBody, receivedPayload, "Hello with Headers");
    assertEquals(headerValue, receivedHeader, "Header value should match");
  }
}
