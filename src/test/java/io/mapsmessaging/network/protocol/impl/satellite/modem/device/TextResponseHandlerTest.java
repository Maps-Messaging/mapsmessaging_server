package io.mapsmessaging.network.protocol.impl.satellite.modem.device;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TextResponseHandlerTest {

  @Test
  void stripsTrailingCrLfBeforeDeliveringText() {
    AtomicReference<String> received = new AtomicReference<>();
    TextResponseHandler handler = new TextResponseHandler(received::set);
    Packet packet = new Packet(32, false);
    packet.put("READY\r\n".getBytes(StandardCharsets.US_ASCII));
    packet.flip();

    handler.onData(packet);

    assertEquals("READY", received.get());
    assertEquals(0, packet.available());
  }

  @Test
  void emptyLineDeliversEmptyString() {
    AtomicReference<String> received = new AtomicReference<>();
    TextResponseHandler handler = new TextResponseHandler(received::set);
    Packet packet = new Packet(2, false);
    packet.put("\r\n".getBytes(StandardCharsets.US_ASCII));
    packet.flip();

    handler.onData(packet);

    assertEquals("", received.get());
  }
}
