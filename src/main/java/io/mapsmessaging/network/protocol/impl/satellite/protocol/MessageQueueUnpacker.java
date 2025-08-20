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

package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class MessageQueueUnpacker {

  private MessageQueueUnpacker() {}

  public static Map<String, List<byte[]>> unpack(byte[] data, boolean compressed) throws IOException {
    byte[] rawBuffer = compressed ? inflate(data) : data;
    if (rawBuffer == null || rawBuffer.length < 2) return Map.of();

    ByteBuffer frameBuffer = ByteBuffer.wrap(rawBuffer);
    if (frameBuffer.remaining() < 2) return Map.of();
    int numberOfNamespaces = Short.toUnsignedInt(frameBuffer.getShort());

    // Preserve on-wire order; allow duplicates to merge
    Map<String, List<byte[]>> namespaceToMessages = new LinkedHashMap<>(Math.max(4, numberOfNamespaces));

    for (int namespaceIndex = 0; namespaceIndex < numberOfNamespaces; namespaceIndex++) {
      if (!unpackNamespaceStream(frameBuffer, namespaceToMessages)) {
        return Map.of(); // malformed frame → fail closed (no partial delivery)
      }
    }
    return namespaceToMessages;
  }

  private static boolean unpackNamespaceStream(ByteBuffer frameBuffer, Map<String, List<byte[]>> namespaceToMessages) {
    if (frameBuffer.remaining() < 2) return false;
    int namespaceStreamLength = Short.toUnsignedInt(frameBuffer.getShort());
    if (frameBuffer.remaining() < namespaceStreamLength) return false;

    // Slice the namespace stream
    int savedLimit = frameBuffer.limit();
    int streamEndPosition = frameBuffer.position() + namespaceStreamLength;
    frameBuffer.limit(streamEndPosition);
    ByteBuffer namespaceBuffer = frameBuffer.slice();
    frameBuffer.position(streamEndPosition);
    frameBuffer.limit(savedLimit);

    // <u16:namespaceLength>
    if (namespaceBuffer.remaining() < 2) return false;
    int namespaceLength = Short.toUnsignedInt(namespaceBuffer.getShort());

    // Need namespace bytes + <u16:numberOfMessages>
    if (namespaceBuffer.remaining() < namespaceLength + 2) return false;

    byte[] namespaceBytes = new byte[namespaceLength];
    namespaceBuffer.get(namespaceBytes);
    String namespace = new String(namespaceBytes, StandardCharsets.UTF_8);

    int numberOfMessages = Short.toUnsignedInt(namespaceBuffer.getShort());
    List<byte[]> messageList = namespaceToMessages.computeIfAbsent(namespace, k -> new ArrayList<>(numberOfMessages));

    for (int messageIndex = 0; messageIndex < numberOfMessages; messageIndex++) {
      if (namespaceBuffer.remaining() < 2) return false;
      int messageLength = Short.toUnsignedInt(namespaceBuffer.getShort());
      if (namespaceBuffer.remaining() < messageLength) return false;

      byte[] messagePayload = new byte[messageLength];
      if (messageLength > 0) {
        namespaceBuffer.get(messagePayload);
      }
      messageList.add(messagePayload);
    }
    return true;
  }

  private static byte[] inflate(byte[] data) throws IOException {
    Inflater inflater = new Inflater(false);
    inflater.setInput(data);

    byte[] tempBuffer = new byte[8192];
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(Math.max(64, data.length * 2))) {
      while (true) {
        int bytesRead;
        try {
          bytesRead = inflater.inflate(tempBuffer);
        } catch (DataFormatException e) {
          return null; // invalid compressed payload
        }

        if (bytesRead > 0) {
          outputStream.write(tempBuffer, 0, bytesRead);
          continue;
        }

        if (inflater.finished()) break;

        // Stuck guard: neither finished nor producing nor needing input/dictionary
        if (!inflater.needsInput() && !inflater.needsDictionary() && bytesRead == 0) {
          return null;
        }

        if (inflater.needsInput()) break; // truncated input
        if (inflater.needsDictionary()) return null; // unsupported dictionary
      }
      return outputStream.toByteArray();
    } finally {
      inflater.end();
    }
  }
}
