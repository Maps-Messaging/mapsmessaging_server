package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.test.BaseTestConfig;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsVerbsTest extends BaseTestConfig {

  private Connection connection;

  @BeforeAll
  void setup() throws Exception {
    Options options = new Options.Builder()
        .server("nats://localhost:4222")
        .connectionTimeout(Duration.ofSeconds(5))
        .build();
    connection = Nats.connect(options);
  }

  @AfterAll
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

    String received = future.get(2, TimeUnit.SECONDS);
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
    connection.flush(Duration.ofSeconds(2));

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
}
