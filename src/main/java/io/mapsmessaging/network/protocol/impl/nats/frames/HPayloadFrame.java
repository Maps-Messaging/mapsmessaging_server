package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public abstract class HPayloadFrame extends NatsFrame {

  protected int maxBufferSize;
  private String subject;
  private String replyTo;
  private String subscriptionId;
  private int headerSize;

  private Map<String, String> header;
  private int payloadSize;
  private byte[] payload;
  private byte[] headerBytes;

  protected HPayloadFrame(int maxBufferSize) {
    super();
    this.maxBufferSize = maxBufferSize;
  }

  public void parseFrame(Packet packet) throws IOException {
    super.parseFrame(packet);

    if (packet.available() < payloadSize + 2) { // plus \r\n
      throw new EndOfBufferException("Incomplete payload for MSG frame");
    }

    payload = new byte[payloadSize-headerSize];
    byte[] header = new byte[headerSize];
    packet.get(header);
    packet.get(payload);

    // Consume the trailing CRLF after payload
    byte cr = packet.get();
    byte lf = packet.get();
    if (cr != '\r' || lf != '\n') {
      throw new IOException("Invalid MSG frame ending");
    }
    String headerLine = new String(header, StandardCharsets.US_ASCII);
    parseHeaders(headerLine);
  }

  private void parseHeaders(String headersBlock) throws NatsProtocolException {
    String[] lines = headersBlock.split("\r\n");
    if (!lines[0].equals("NATS/1.0")) {
      throw new NatsProtocolException("Invalid headers block start");
    }
    header = new LinkedHashMap<>();
    for (int i = 1; i < lines.length; i++) {
      String line = lines[i];
      if (line.isEmpty()) {
        break; // End of headers
      }
      int colonIdx = line.indexOf(':');
      if (colonIdx <= 0) {
        throw new NatsProtocolException("Invalid header line: " + line);
      }
      String key = line.substring(0, colonIdx).trim();
      String value = line.substring(colonIdx + 1).trim();

      // Store header key/value (Maps style)
      header.put(key, value);
    }
  }

  @Override
  public void parseLine(String line) throws NatsProtocolException {
    String[] parts = line.trim().split(" ");

    if (parts.length < 3 || parts.length > 4) {
      throw new NatsProtocolException("Invalid HPUB frame header: " + line);
    }

    subject = parts[0];

    if (parts.length == 3) {
      // No reply-to
      replyTo = null;
      headerSize = Integer.parseInt(parts[1]);
      payloadSize = Integer.parseInt(parts[2]);
    } else {
      // reply-to present
      replyTo = parts[1];
      headerSize = Integer.parseInt(parts[2]);
      payloadSize = Integer.parseInt(parts[3]);
    }

    if (payloadSize > maxBufferSize) {
      throw new NatsProtocolException("Payload size exceeds max buffer size");
    }

    if (headerSize > payloadSize) {
      throw new NatsProtocolException("Header size larger than total payload size");
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

    if(subscriptionId != null && !subscriptionId.isEmpty()) {
      packet.put(subscriptionId.getBytes(StandardCharsets.US_ASCII));
      packet.put((byte) ' ');
    }

    if (replyTo != null && !replyTo.isEmpty()) {
      packet.put(replyTo.getBytes(StandardCharsets.US_ASCII));
      packet.put((byte) ' ');
    }

    if(payload == null)payload = new byte[0];
    if(headerBytes == null)headerBytes = new byte[0];

    int length = headerBytes.length + payload.length;
    packet.put(Integer.toString(headerBytes.length).getBytes(StandardCharsets.US_ASCII));
    packet.put((byte) ' ');
    packet.put(Integer.toString(length).getBytes(StandardCharsets.US_ASCII));
    packet.put("\r\n".getBytes(StandardCharsets.US_ASCII));

    if(headerBytes.length > 0) {
      packet.put(headerBytes);
    }
    // Write payload (if present)
    if (payload.length > 0) {
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

}
