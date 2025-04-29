package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.test.BaseTestConfig;
import io.nats.client.*;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsJetStreamLikeTest extends BaseTestConfig {

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
  void testDurableSubWithManualAckSimulation() throws Exception {
    String subject = "js.manual.ack";
    CompletableFuture<String> acked = new CompletableFuture<>();

    Dispatcher dispatcher = connection.createDispatcher(msg -> {
      // Simulate manual ack by replying to reply-to (e.g., _INBOX.abc)
      if (msg.getReplyTo() != null) {
        connection.publish(msg.getReplyTo(), "+ACK".getBytes());
        acked.complete("acked");
      }
    });
    dispatcher.subscribe(subject);

    // Publish with a reply-to inbox (manual ack trigger)
    String inbox = "_INBOX." + UUID.randomUUID();
    connection.publish(subject, inbox, "Need ACK".getBytes());
    connection.flush(Duration.ofSeconds(2));

    String result = acked.get(3, TimeUnit.SECONDS);
    assertEquals("acked", result);
  }

  @Test
  @Order(2)
  void testReplaySimulation() throws Exception {
    String subject = "js.replay";
    String message = "Replay test";

    CompletableFuture<String> firstReceive = new CompletableFuture<>();
    Dispatcher dispatcher = connection.createDispatcher(msg -> firstReceive.complete(new String(msg.getData())));
    dispatcher.subscribe(subject);

    connection.publish(subject, message.getBytes());
    connection.flush(Duration.ofSeconds(1));

    String received = firstReceive.get(2, TimeUnit.SECONDS);
    assertEquals(message, received);

    // Simulate replay (manual resend)
    CompletableFuture<String> secondReceive = new CompletableFuture<>();
    dispatcher.unsubscribe(subject);
    Dispatcher newDispatcher = connection.createDispatcher(msg -> secondReceive.complete(new String(msg.getData())));
    newDispatcher.subscribe(subject);

    connection.publish(subject, message.getBytes()); // Replay event
    connection.flush(Duration.ofSeconds(1));

    String replayed = secondReceive.get(2, TimeUnit.SECONDS);
    assertEquals(message, replayed);
  }

  @Test
  @Order(3)
  void testMsgWithInboxReplyRouting() throws Exception {
    String subject = "js.inbox.reply";
    String replyTo = "_INBOX." + UUID.randomUUID();

    CompletableFuture<String> inboxResponder = new CompletableFuture<>();

    // Listen on the replyTo
    Dispatcher responder = connection.createDispatcher(msg -> {
      inboxResponder.complete(new String(msg.getData()));
    });
    responder.subscribe(replyTo);

    connection.publish(subject, replyTo, "Send me a reply".getBytes());
    connection.flush(Duration.ofSeconds(2));

    // Simulate reply
    connection.publish(replyTo, "Replying back".getBytes());
    connection.flush(Duration.ofSeconds(2));

    String reply = inboxResponder.get(3, TimeUnit.SECONDS);
    assertEquals("Replying back", reply);
  }
}
