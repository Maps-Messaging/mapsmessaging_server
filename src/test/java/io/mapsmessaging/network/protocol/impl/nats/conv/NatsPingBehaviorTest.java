package io.mapsmessaging.network.protocol.impl.nats.conv;

import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsPingBehaviorTest extends BaseTestConfig {

  private static final int PING_TEST_PORT = 5222;
  private static final Pattern pingRe = Pattern.compile("PING");
  private static final Pattern errRe = Pattern.compile("-ERR");

  private NatsTestHelpers helper;

  @BeforeEach
  void setup() throws Exception {
    helper = new NatsTestHelpers("127.0.0.1", PING_TEST_PORT);
  }

  @AfterEach
  void teardown() throws Exception {
    helper.close();
  }

  @Test
  @Order(1)
  void testPingIntervalAndDisconnect() throws Exception {
    for (int i = 0; i < 2; i++) {
      helper.expect(pingRe, 1000);
    }

    helper.expect(errRe, 1000); // should error after no PONGs

// Wait until server closes connection
    boolean closed = false;
    try {
      int result = helper.getReader().read(); // should return -1
      closed = (result == -1);
    } catch (IOException e) {
      closed = true; // socket forcibly closed (RST)
    }
    assertTrue(closed, "Expected server to close the connection");
  }

  @Test
  @Order(2)
  void testUnpromptedPongIgnored() throws Exception {
    // Send many unsolicited PONGs
    for (int i = 0; i < 100; i++) {
      helper.send("PONG\r\n");
    }

    // Still expect PINGs from server as if no PONGs were received
    for (int i = 0; i < 2; i++) {
      helper.expect(pingRe, 1000);
    }

    helper.expect(errRe, 1000);

    boolean closed = false;
    try {
      int result = helper.getReader().read(); // should return -1
      closed = (result == -1);
    } catch (IOException e) {
      closed = true; // socket forcibly closed (RST)
    }
    assertTrue(closed, "Expected server to close the connection");
  }
}
