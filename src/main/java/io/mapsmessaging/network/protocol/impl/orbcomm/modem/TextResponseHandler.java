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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem;

import io.mapsmessaging.network.io.Packet;

import java.util.function.Consumer;

public class TextResponseHandler implements ModemLineHandler {
  private final Consumer<String> lineConsumer;
  private final StringBuilder lineBuffer = new StringBuilder();

  public TextResponseHandler(Consumer<String> lineConsumer) {
    this.lineConsumer = lineConsumer;
  }

  @Override
  public void onData(Packet packet) {
    while (packet.hasRemaining()) {
      byte b = packet.get();
      if (b == '\n') {
        String line = lineBuffer.toString().trim();
        lineBuffer.setLength(0);
        if (!line.isEmpty()) lineConsumer.accept(line);
      } else if (b != '\r') {
        lineBuffer.append((char) b);
      }
    }
  }
}
