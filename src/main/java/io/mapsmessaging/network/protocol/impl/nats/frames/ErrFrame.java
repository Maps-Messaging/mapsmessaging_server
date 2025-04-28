package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.Setter;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class ErrFrame extends NatsFrame {

  private String error;

  public ErrFrame() {
    super();
  }

  @Override
  public byte[] getCommand() {
    return "-ERR".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  protected void parseLine(String line) {
    // NATS -ERR '<error text>'
    int firstQuote = line.indexOf('\'');
    int lastQuote = line.lastIndexOf('\'');
    if (firstQuote >= 0 && lastQuote > firstQuote) {
      error = line.substring(firstQuote + 1, lastQuote);
    } else {
      error = "Unknown Error Format";
    }
  }

  @Override
  public int packFrame(Packet packet) {
    int start = packet.position();
    packet.put(getCommand());
    packet.put((byte) ' ');
    packet.put(("'" + (error != null ? error : "Unknown Error") + "'").getBytes(StandardCharsets.US_ASCII));
    packet.put("\r\n".getBytes(StandardCharsets.US_ASCII));
    return packet.position() - start;
  }

  @Override
  public boolean isValid() {
    return error != null;
  }

  @Override
  public NatsFrame instance() {
    return new ErrFrame();
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
