/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.stream;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SimpleJsonWriter {

  public static void main(String[] args) throws Exception {

    String host = "localhost";
    int port = 9999;

    try (Socket socket = new Socket(host, port)) {

      OutputStream outputStream = socket.getOutputStream();

      int counter = 0;

      while (true) {

        String json =
            "{"
                + "\"id\":" + counter + ","
                + "\"timestamp\":" + System.currentTimeMillis()
                + "}";

        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        outputStream.write(data);
        outputStream.flush();

        System.out.println("Sent: " + json);

        counter++;

        Thread.sleep(100);
      }
    }
  }
}
