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

import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import lombok.Getter;
import lombok.ToString;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS UNSUB frame from client.
 */
@Getter
@ToString
public class UnsubFrame extends NatsFrame {

  private String subscriptionId;
  private Integer maxMessages;

  public UnsubFrame() {
    super();
  }

  @Override
  public byte[] getCommand() {
    return "UNSUB".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  protected void parseLine(String line) throws NatsProtocolException {
    String[] parts = line.trim().split(" ");
    if (parts.length < 1 || parts.length > 2) {
      throw new NatsProtocolException("Invalid UNSUB frame header: " + line);
    }
    subscriptionId = parts[0];
    if (parts.length == 2) {
      maxMessages = Integer.parseInt(parts[1]);
    } else {
      maxMessages = null;
    }
  }

  @Override
  public boolean isValid() {
    return subscriptionId != null;
  }

  @Override
  public NatsFrame instance() {
    return new UnsubFrame();
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
