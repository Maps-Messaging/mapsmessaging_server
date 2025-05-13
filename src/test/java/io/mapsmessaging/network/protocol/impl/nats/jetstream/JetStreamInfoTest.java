package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JetStreamInfoTest extends JetStreamBaseTest {

  @Test
  void testJetStreamInfoHelper() throws Exception {
    JsonObject info = requestJetStreamInfo();
    assertNotNull(info);
    assertTrue(info.has("config"));
    assertTrue(info.getAsJsonObject("config").has("api_prefix"));
  }


}
