package io.mapsmessaging.network.protocol.impl.nats.frames;

import lombok.Getter;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS INFO frame from server.
 */
@Getter
public class InfoFrame extends NatsFrame {

  private String serverId;
  private String version;
  private String host;
  private boolean tlsRequired = false;
  private boolean authRequired = false;

  public InfoFrame() {
    super();
  }

  @Override
  public byte[] getCommand() {
    return "INFO".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  protected void parseLine(String json) {
    // Manual parsing, consistent with ConnectFrame style
    this.serverId = extractString(json, "\"server_id\":");
    this.version = extractString(json, "\"version\":");
    this.host = extractString(json, "\"host\":");
    this.tlsRequired = extractBoolean(json, "\"tls_required\":");
    this.authRequired = extractBoolean(json, "\"auth_required\":");
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public NatsFrame instance() {
    return new InfoFrame();
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
