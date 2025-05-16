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

import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS CONNECT frame from client.
 */
@Getter
@Setter
@ToString
public class ConnectFrame extends NatsFrame {

  private String user;
  private String pass;
  private boolean tlsRequired = false;
  private boolean verbose = false;
  private boolean pedantic = false;
  private boolean echo = true;
  private boolean headers = false;

  public ConnectFrame() {
    super();
  }

  @Override
  public byte[] getCommand() {
    return "CONNECT".getBytes(StandardCharsets.US_ASCII);
  }

  protected void parseLine(String line) {
    this.echo = extractBoolean(line, "\"echo\":");
    this.headers = extractBoolean(line, "\"headers\":");
    this.verbose = extractBoolean(line, "\"verbose\":");
    this.pedantic = extractBoolean(line, "\"pedantic\":");
    this.tlsRequired = extractBoolean(line, "\"tls_required\":");
    this.user = extractString(line, "\"user\":");
    this.pass = extractString(line, "\"pass\":");
  }

  @Override
  public int packFrame(Packet packet) {
    int start = packet.position();

    // Write "CONNECT "
    packet.put(getCommand());
    packet.put((byte) ' ');

    // Build JSON manually (no external libraries)
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append('{');
    jsonBuilder.append("\"verbose\":").append(verbose).append(',');
    jsonBuilder.append("\"pedantic\":").append(pedantic).append(',');
    jsonBuilder.append("\"tls_required\":").append(tlsRequired);

    if (user != null && !user.isEmpty()) {
      jsonBuilder.append(",\"user\":\"").append(user).append('\"');
    }
    if (pass != null && !pass.isEmpty()) {
      jsonBuilder.append(",\"pass\":\"").append(pass).append('\"');
    }

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
    return new ConnectFrame();
  }

}
