package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS INFO frame from server.
 */
@Getter
@Setter
@ToString
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
    this.serverId = extractString(json, "\"server_id\":");
    this.version = extractString(json, "\"version\":");
    this.host = extractString(json, "\"host\":");
    this.tlsRequired = extractBoolean(json, "\"tls_required\":");
    this.authRequired = extractBoolean(json, "\"auth_required\":");
  }

  @Override
  public int packFrame(Packet packet) {
    int start = packet.position();

    // Write "INFO "
    packet.put(getCommand());
    packet.put((byte) ' ');

    // Build JSON manually (simple string builder)
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append('{');

    boolean first = true;

    if (serverId != null) {
      jsonBuilder.append("\"server_id\":\"").append(serverId).append('\"');
      first = false;
    }
    if (version != null) {
      if (!first) jsonBuilder.append(',');
      jsonBuilder.append("\"version\":\"").append(version).append('\"');
      first = false;
    }
    if (host != null) {
      if (!first) jsonBuilder.append(',');
      jsonBuilder.append("\"host\":\"").append(host).append('\"');
      first = false;
    }

    if (!first) jsonBuilder.append(',');
    jsonBuilder.append("\"tls_required\":").append(tlsRequired);
    jsonBuilder.append(',');
    jsonBuilder.append("\"auth_required\":").append(authRequired);

    jsonBuilder.append('}');

    // Write the JSON
    packet.put(jsonBuilder.toString().getBytes(StandardCharsets.US_ASCII));

    // Write final CRLF
    packet.put("\r\n".getBytes(StandardCharsets.US_ASCII));

    return packet.position() - start;
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
