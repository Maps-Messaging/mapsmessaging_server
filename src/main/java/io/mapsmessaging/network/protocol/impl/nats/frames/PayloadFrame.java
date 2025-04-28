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

  public PayloadFrame(int maxBufferSize) {
    super();
    this.maxBufferSize = maxBufferSize;
  }

  @Override
  public byte[] getCommand() {
    return "MSG".getBytes(StandardCharsets.US_ASCII);
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
    if (parts.length < 4) {
      throw new NatsProtocolException("Invalid MSG frame header: " + line);
    }

    subject = parts[1];
    subscriptionId = parts[2];

    if (parts.length == 5) {
      replyTo = parts[3];
      payloadSize = Integer.parseInt(parts[4]);
    } else {
      replyTo = null;
      payloadSize = Integer.parseInt(parts[3]);
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
    return subject != null && subscriptionId != null;
  }

  @Override
  public NatsFrame instance() {
    return new MsgFrame(maxBufferSize);
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
