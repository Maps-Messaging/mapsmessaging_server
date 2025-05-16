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
import io.nats.client.impl.Headers;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsHeadersTest extends BaseTestConfig {

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
  void testHeaderDelivery() throws Exception {
    String subject = "headers.basic";
    String key = "hkey";
    String val = "hval";

    CompletableFuture<Message> future = new CompletableFuture<>();
    Dispatcher dispatcher = connection.createDispatcher(future::complete);
    dispatcher.subscribe(subject);

    Headers headers = new Headers();
    headers.add(key, val);
    connection.publish(subject, null, headers, "HeaderTest".getBytes());
    connection.flush(Duration.ofSeconds(2));

    Message msg = future.get(3, TimeUnit.SECONDS);
    assertEquals("HeaderTest", new String(msg.getData()));
    assertTrue(msg.hasHeaders());
    assertEquals(val, msg.getHeaders().getFirst(key));
  }

  @Test
  @Order(2)
  void testMultipleHeaders() throws Exception {
    String subject = "headers.multi";

    Headers headers = new Headers();
    headers.add("alpha", "1");
    headers.add("beta", "2");

    CompletableFuture<Message> future = new CompletableFuture<>();
    Dispatcher dispatcher = connection.createDispatcher(future::complete);
    dispatcher.subscribe(subject);

    connection.publish(subject, null, headers, "MultiHeader".getBytes());
    connection.flush(Duration.ofSeconds(2));

    Message msg = future.get(3, TimeUnit.SECONDS);
    assertTrue(msg.hasHeaders());
    assertEquals("1", msg.getHeaders().getFirst("alpha"));
    assertEquals("2", msg.getHeaders().getFirst("beta"));
  }

  @Test
  @Order(3)
  void testHeaderOnlyMessage() throws Exception {
    String subject = "headers.only";
    Headers headers = new Headers();
    headers.add("flag", "true");

    CompletableFuture<Message> future = new CompletableFuture<>();
    Dispatcher dispatcher = connection.createDispatcher(future::complete);
    dispatcher.subscribe(subject);

    connection.publish(subject, null, headers, new byte[0]);
    connection.flush(Duration.ofSeconds(2));

    Message msg = future.get(3, TimeUnit.SECONDS);
    assertTrue(msg.hasHeaders());
    assertEquals("true", msg.getHeaders().getFirst("flag"));
    assertEquals(0, msg.getData().length);
  }

  @Test
  @Order(4)
  void testHeaderSpecialChars() throws Exception {
    String subject = "headers.special";
    String key = "content-type";
    String val = "text/plain; charset=UTF-8";

    Headers headers = new Headers();
    headers.add(key, val);

    CompletableFuture<Message> future = new CompletableFuture<>();
    Dispatcher dispatcher = connection.createDispatcher(future::complete);
    dispatcher.subscribe(subject);

    connection.publish(subject, null, headers, "Encoding Test".getBytes());
    connection.flush(Duration.ofSeconds(2));

    Message msg = future.get(3, TimeUnit.SECONDS);
    assertEquals("Encoding Test", new String(msg.getData()));
    assertEquals(val, msg.getHeaders().getFirst(key));
  }
}
