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

package io.mapsmessaging.test;

import io.mapsmessaging.network.io.Packet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ProtocolFragmentationTestSupport {

  private ProtocolFragmentationTestSupport() {
  }

  @FunctionalInterface
  public interface PacketProcessor {
    void process(Packet packet) throws Exception;
  }

  public static void feed(byte[] data, int chunkSize, PacketProcessor processor) throws Exception {
    if (chunkSize <= 0) {
      throw new IllegalArgumentException("chunkSize must be greater than zero");
    }

    Packet packet = new Packet(Math.max(65536, data.length + 1024), false);
    int offset = 0;
    while (offset < data.length) {
      int length = Math.min(chunkSize, data.length - offset);
      packet.put(data, offset, length);
      offset += length;
      packet.flip();
      processor.process(packet);
      prepareForNextRead(packet);
    }
  }

  public static void feed(byte[] data, int firstChunkSize, int secondChunkSize, PacketProcessor processor) throws Exception {
    if (firstChunkSize <= 0 || firstChunkSize >= data.length) {
      throw new IllegalArgumentException("firstChunkSize must split the input");
    }
    if (secondChunkSize <= 0) {
      throw new IllegalArgumentException("secondChunkSize must be greater than zero");
    }

    Packet packet = new Packet(Math.max(65536, data.length + 1024), false);
    packet.put(data, 0, firstChunkSize);
    packet.flip();
    processor.process(packet);
    prepareForNextRead(packet);

    int offset = firstChunkSize;
    while (offset < data.length) {
      int length = Math.min(secondChunkSize, data.length - offset);
      packet.put(data, offset, length);
      offset += length;
      packet.flip();
      processor.process(packet);
      prepareForNextRead(packet);
    }
  }

  public static byte[] readHexResourceLine(Class<?> owner, String resourceName, int lineNumber) throws IOException {
    if (lineNumber < 0) {
      throw new IllegalArgumentException("lineNumber must not be negative");
    }

    InputStream inputStream = Objects.requireNonNull(owner.getResourceAsStream(resourceName), "Unable to locate " + resourceName);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.US_ASCII))) {
      String line = null;
      for (int index = 0; index <= lineNumber; index++) {
        line = reader.readLine();
        if (line == null) {
          throw new IOException("Resource " + resourceName + " does not contain line " + lineNumber);
        }
      }
      return parseHexLine(line);
    }
  }

  private static byte[] parseHexLine(String line) {
    String[] values = line.split(",");
    byte[] bytes = new byte[values.length];
    int count = 0;
    for (String value : values) {
      String trimmed = value.trim();
      if (!trimmed.isEmpty()) {
        bytes[count++] = (byte) Integer.parseInt(trimmed, 16);
      }
    }
    if (count == bytes.length) {
      return bytes;
    }
    byte[] result = new byte[count];
    System.arraycopy(bytes, 0, result, 0, count);
    return result;
  }

  private static void prepareForNextRead(Packet packet) {
    if (packet.position() == packet.limit()) {
      packet.clear();
    } else if (packet.position() < packet.limit()) {
      if (packet.position() != 0) {
        packet.compact();
      } else {
        packet.position(packet.limit());
        packet.limit(packet.capacity());
      }
    } else {
      packet.clear();
    }
  }
}
