package io.mapsmessaging.network.protocol.impl.websockets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.network.protocol.impl.websockets.frames.Frame;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("conformance")
@Tag("conformance-core")
@Tag("websocket")
class WebSocketHandshakeConformanceTest {

  private static final String SPEC = "WebSocket RFC 6455";
  private static final String SOURCE = ProtocolRequirement.WEBSOCKET_RFC6455_SOURCE;
  private static final String KEY = "dGhlIHNhbXBsZSBub25jZQ==";

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §4.2.1-§4.2.2: valid HTTP/1.1 Upgrade handshake returns 101 headers and Sec-WebSocket-Accept",
      source = SOURCE)
  void validUpgradeProducesExpectedAcceptKey() throws Exception {
    Connecting connecting = new Connecting();
    Frame response = connecting.handle(packet(request("GET /ws HTTP/1.1", "Host: example.test\r\n"
        + "Upgrade: websocket\r\nConnection: keep-alive, Upgrade\r\n"
        + "Sec-WebSocket-Key: " + KEY + "\r\nSec-WebSocket-Version: 13\r\n")), endpoint());
    assertNotNull(response);
    assertEquals("websocket", response.getHeaders().get("Upgrade"));
    assertEquals("Upgrade", response.getHeaders().get("Connection"));
    assertEquals("s3pPLMBiTxaQ9kYGzzhZRbK+xOo=", response.getHeaders().get("Sec-WebSocket-Accept"));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §4.2.1: opening handshake method MUST be GET and HTTP version MUST be at least 1.1",
      source = SOURCE)
  void nonGetOrHttp10HandshakeIsRejected() {
    assertThrows(IOException.class, () -> new Connecting().handle(packet(request("POST /ws HTTP/1.1", baseHeaders())), endpoint()));
    assertThrows(IOException.class, () -> new Connecting().handle(packet(request("GET /ws HTTP/1.0", baseHeaders())), endpoint()));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §4.2.1: Host, Upgrade:websocket, Connection:Upgrade, version 13 and Sec-WebSocket-Key are required",
      source = SOURCE)
  void requiredHandshakeHeadersAreEnforced() {
    String[] headers = {
        "Upgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: " + KEY + "\r\nSec-WebSocket-Version: 13\r\n",
        "Host: example.test\r\nConnection: Upgrade\r\nSec-WebSocket-Key: " + KEY + "\r\nSec-WebSocket-Version: 13\r\n",
        "Host: example.test\r\nUpgrade: websocket\r\nSec-WebSocket-Key: " + KEY + "\r\nSec-WebSocket-Version: 13\r\n",
        "Host: example.test\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Version: 13\r\n",
        "Host: example.test\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: " + KEY + "\r\n"
    };
    for (String header : headers) {
      assertThrows(IOException.class, () -> new Connecting().handle(packet(request("GET /ws HTTP/1.1", header)), endpoint()));
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §4.2.1: Sec-WebSocket-Key is Base64 encoded 16-byte nonce and version is 13",
      source = SOURCE)
  void invalidKeyAndVersionAreRejected() {
    assertThrows(IOException.class, () -> new Connecting().handle(packet(request("GET /ws HTTP/1.1",
        baseHeaders().replace(KEY, "not-base64!"))), endpoint()));
    String shortKey = java.util.Base64.getEncoder().encodeToString(new byte[15]);
    assertThrows(IOException.class, () -> new Connecting().handle(packet(request("GET /ws HTTP/1.1",
        baseHeaders().replace(KEY, shortKey))), endpoint()));
    assertThrows(IOException.class, () -> new Connecting().handle(packet(request("GET /ws HTTP/1.1",
        baseHeaders().replace("Sec-WebSocket-Version: 13", "Sec-WebSocket-Version: 12"))), endpoint()));
  }

  @Test
  @Disabled("Known WebSocket HTTP upgrade line-termination gap: MSG-370")
  @ProtocolRequirement(
      specification = SPEC,
      value = "RFC 6455 §4.2.1 uses HTTP/1.1 opening handshake syntax with CRLF line termination",
      source = SOURCE)
  void lfOnlyHandshakeIsRejected() {
    String request = ("GET /ws HTTP/1.1\n"
        + "Host: example.test\nUpgrade: websocket\nConnection: Upgrade\n"
        + "Sec-WebSocket-Key: " + KEY + "\nSec-WebSocket-Version: 13\n\n");
    assertThrows(IOException.class, () -> new Connecting().handle(packet(request), endpoint()));
  }

  private static String baseHeaders() {
    return "Host: example.test\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
        + "Sec-WebSocket-Key: " + KEY + "\r\nSec-WebSocket-Version: 13\r\n";
  }

  private static String request(String requestLine, String headers) {
    return requestLine + "\r\n" + headers + "\r\n";
  }

  private static Packet packet(String value) {
    return new Packet(ByteBuffer.wrap(value.getBytes(StandardCharsets.US_ASCII)));
  }

  private static EndPoint endpoint() {
    EndPoint endPoint = mock(EndPoint.class, RETURNS_DEEP_STUBS);
    when(endPoint.getServer().getConfig().getProtocols()).thenReturn("mqtt,stomp,amqp");
    return endPoint;
  }
}
