/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public abstract class HPayloadFrame extends PayloadFrame {

  private int headerSize;
  private Map<String, String> header;
  private byte[] headerBytes;

  protected HPayloadFrame(int maxBufferSize) {
    super(maxBufferSize);
  }

  protected PayloadFrame copy(PayloadFrame frame) {
    HPayloadFrame hframe = (HPayloadFrame) super.copy(frame);
    hframe.header = header;
    hframe.headerBytes = headerBytes;
    hframe.headerSize = headerSize;
    return frame;
  }

  @Override
  public void parseFrame(Packet packet) throws IOException {

    parseLine(extractLine(packet));

    if (packet.available() < payloadSize + 2) { // plus \r\n
      throw new EndOfBufferException("Incomplete payload for MSG frame");
    }

    payload = new byte[payloadSize - headerSize];
    headerBytes = new byte[headerSize];
    packet.get(headerBytes);
    packet.get(payload);

    // Consume the trailing CRLF after payload
    byte cr = packet.get();
    byte lf = packet.get();
    if (cr != '\r' || lf != '\n') {
      throw new IOException("Invalid MSG frame ending");
    }
    String headerLine = new String(headerBytes, StandardCharsets.US_ASCII);
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

    if (subscriptionId != null && !subscriptionId.isEmpty()) {
      packet.put(subscriptionId.getBytes(StandardCharsets.US_ASCII));
      packet.put((byte) ' ');
    }

    if (replyTo != null && !replyTo.isEmpty()) {
      packet.put(replyTo.getBytes(StandardCharsets.US_ASCII));
      packet.put((byte) ' ');
    }

    if (payload == null) payload = new byte[0];
    if (headerBytes == null) headerBytes = new byte[0];

    int length = headerBytes.length + payload.length;
    packet.put(Integer.toString(headerBytes.length).getBytes(StandardCharsets.US_ASCII));
    packet.put((byte) ' ');
    packet.put(Integer.toString(length).getBytes(StandardCharsets.US_ASCII));
    packet.put("\r\n".getBytes(StandardCharsets.US_ASCII));

    if (headerBytes.length > 0) {
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

}
