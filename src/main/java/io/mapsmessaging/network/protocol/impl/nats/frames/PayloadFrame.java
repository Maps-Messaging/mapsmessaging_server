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
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@ToString
public abstract class PayloadFrame extends NatsFrame {

  protected int maxBufferSize;
  protected String subject;
  protected String subscriptionId;
  protected String replyTo;
  protected int payloadSize;
  protected byte[] payload;

  protected PayloadFrame(int maxBufferSize) {
    super();
    this.maxBufferSize = maxBufferSize;
  }


  public abstract PayloadFrame duplicate();

  protected PayloadFrame copy(PayloadFrame frame) {
    frame.setSubject(subject);
    frame.setSubscriptionId(subscriptionId);
    frame.setReplyTo(replyTo);
    frame.setPayloadSize(payloadSize);
    frame.setPayload(payload);
    frame.setMaxBufferSize(maxBufferSize);
    return frame;
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


    if (subscriptionId != null && !subscriptionId.isEmpty()) {
      packet.put(subscriptionId.getBytes(StandardCharsets.US_ASCII));
      packet.put((byte) ' ');
    }

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
