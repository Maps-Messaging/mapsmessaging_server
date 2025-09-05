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

package io.mapsmessaging.network.protocol.impl.nats.conv;

import lombok.Getter;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

public class NatsTestHelpers {

  private final Socket socket;
  @Getter
  private final BufferedReader reader;
  private final BufferedWriter writer;

  public NatsTestHelpers(String host, int port) throws IOException {
    this.socket = new Socket(host, port);
    this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

    // Read server INFO line
    String info = reader.readLine();
    if (info == null || !info.startsWith("INFO")) {
      throw new IOException("Expected INFO line, got: " + info);
    }

    // Send basic CONNECT (no auth, no TLS)
    writer.write("CONNECT {\"verbose\":true,\"pedantic\":true,\"tls_required\":false,\"echo\":true}\r\n");
    writer.flush();

    // Consume optional +OK
    reader.mark(1024);
    String maybeOk = reader.readLine();
    if (maybeOk != null && !maybeOk.startsWith("+OK")) {
      reader.reset(); // Put it back if not +OK
    }
  }

  public void send(String data) throws IOException {
    writer.write(data);
    writer.flush();
  }

  public String expect(Pattern pattern, long timeoutMillis) throws IOException {
    long deadline = System.currentTimeMillis() + timeoutMillis;

    while (System.currentTimeMillis() < deadline) {
      if (reader.ready()) {
        String line = reader.readLine();
        if (line == null) break;
        Matcher m = pattern.matcher(line);
        if (m.find()) return line;
      } else {
        try {
          Thread.sleep(10);
        } catch (InterruptedException ignored) {}
      }
    }

    fail("Expected pattern not found within timeout: " + pattern);
    return null;
  }

  public List<String> expectAllMsgs(int expected, int timeoutMillis) throws IOException {
    List<String> msgs = new ArrayList<>();
    long start = System.currentTimeMillis();

    while (msgs.size() < expected && (System.currentTimeMillis() - start) < timeoutMillis) {
      if (reader.ready()) {
        String line = reader.readLine();
        if (line != null) {
          msgs.add(line);
        }
      }
    }
    assertEquals(expected, msgs.size(), "Expected number of MSG frames");
    return msgs;
  }


  public List<String> expectMsgs(int expected, long timeoutMillis) throws IOException {
    List<String> msgs = new ArrayList<>();
    long start = System.currentTimeMillis();

    while (msgs.size() < expected && (System.currentTimeMillis() - start) < timeoutMillis) {
      if (reader.ready()) {
        String line = reader.readLine();
        if (line != null && line.startsWith("MSG ")) {
          msgs.add(line);
          reader.readLine(); // Skip payload line
        }
      }
    }

    assertEquals(expected, msgs.size(), "Expected number of MSG frames");
    return msgs;
  }

  public void close() throws IOException {
    socket.close();
  }
}
