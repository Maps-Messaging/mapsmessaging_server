package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import lombok.Data;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

@Data
public abstract class PayloadFrame extends NatsFrame {

  protected int maxBufferSize;
  private String subject;
  private String subscriptionId;
  private String replyTo;
  private int payloadSize;
  private byte[] payload;

  protected PayloadFrame(int maxBufferSize) {
    super();
    this.maxBufferSize = maxBufferSize;
  }

  public void parseFrame(Packet packet) throws IOException {
    super.parseFrame(packet);

    if (packet.available() < payloadSize + 2) { // plus \r\n
      throw new EndOfBufferException("Incomplete payload for MSG frame");
    }

    payload = new byte[payloadSize];
    packet.get(payload);

    // Consume the trailing CRLF after payload
    byte cr = packet.get();
    byte lf = packet.get();
    if (cr != '\r' || lf != '\n') {
      throw new IOException("Invalid MSG frame ending");
    }
  }

  @Override
  public void parseLine(String line) throws NatsProtocolException {
    String[] parts = line.trim().split(" ");

    if (parts.length < 2) {
      throw new NatsProtocolException("Invalid PUB frame header: " + line);
    }

    subject = parts[0];

    if (parts.length == 2) {
      // No reply-to
      replyTo = null;
      payloadSize = Integer.parseInt(parts[1]);
    } else if (parts.length == 3) {
      // reply-to present
      replyTo = parts[1];
      payloadSize = Integer.parseInt(parts[2]);
    } else {
      throw new NatsProtocolException("Invalid PUB frame header: " + line);
    }

    if (payloadSize > maxBufferSize) {
      throw new NatsProtocolException("Payload size exceeds max buffer size");
    }
  }

  @Override
  public int packFrame(Packet packet) {
    int start = packet.position();

    // Write the verb
    packet.put(getCommand());
    packet.put((byte) ' ');

    // Write the header
    packet.put(subject.getBytes(StandardCharsets.US_ASCII));
    packet.put((byte) ' ');

    if (replyTo != null && !replyTo.isEmpty()) {
      packet.put(replyTo.getBytes(StandardCharsets.US_ASCII));
      packet.put((byte) ' ');
    }

    packet.put(Integer.toString(payload != null ? payload.length : 0).getBytes(StandardCharsets.US_ASCII));
    packet.put("\r\n".getBytes(StandardCharsets.US_ASCII));

    // Write payload (if present)
    if (payload != null && payload.length > 0) {
      packet.put(payload);
    }

    // Always end with CRLF
    packet.put("\r\n".getBytes(StandardCharsets.US_ASCII));

    return packet.position() - start;
  }


  @Override
  public boolean isValid() {
    return subject != null;
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
