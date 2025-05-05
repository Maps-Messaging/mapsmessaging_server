package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.test.BaseTestConfig;
import io.nats.client.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

class JetStreamBaseTest extends BaseTestConfig {

  protected Connection natsConnection;
  protected JetStream jetStream;

  @BeforeEach
  void setupJetStream() throws Exception {
    Options options = new Options.Builder()
        .server("nats://localhost:4222")
        .connectionName("JetStreamTest")
        .noNoResponders()
        .build();

    natsConnection = Nats.connect(options);
    jetStream = natsConnection.jetStream();
  }

  @AfterEach
  void teardownJetStream() throws Exception {
    if (natsConnection != null) {
      natsConnection.close();
    }
  }


  protected JsonObject requestJetStreamInfo() throws Exception {
    natsConnection.flush(Duration.ofSeconds(1));
    Message msg = natsConnection.request("$JS.API.INFO", null, Duration.ofSeconds(4));
    if (msg == null || msg.getData() == null) return null;
    return JsonParser.parseString(new String(msg.getData(), StandardCharsets.UTF_8)).getAsJsonObject();
  }

}
