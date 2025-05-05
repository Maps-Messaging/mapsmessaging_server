package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import com.google.gson.JsonObject;
import io.nats.client.Connection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JetStreamConnectTest extends JetStreamBaseTest {

  @Test
  void testJetStreamConnection() throws Exception {
    assertNotNull(natsConnection);
    assertNotNull(jetStream);
    assertTrue(natsConnection.getStatus() == Connection.Status.CONNECTED);
  }

  @Test
  void testJetStreamInfoHelper() throws Exception {
    JsonObject info = requestJetStreamInfo();
    assertNotNull(info);
    assertTrue(info.has("config"));
    assertTrue(info.getAsJsonObject("config").has("api_prefix"));
  }


}
