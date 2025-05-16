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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@ToString
public class ErrFrame extends NatsFrame {

  private String error;

  public ErrFrame() {
    super();
  }

  public ErrFrame(String error) {
    super();
    this.error = error;
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
