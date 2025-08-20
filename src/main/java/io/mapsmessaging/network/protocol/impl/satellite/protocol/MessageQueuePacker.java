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
import java.util.zip.Deflater;

public final class MessageQueuePacker {

  private static final int MAX_U16 = 0xFFFF;

  private MessageQueuePacker() {}

  public record Packed(byte[] data, boolean compressed) {}

  public static Packed pack(Map<String, List<byte[]>> queuedMessages, int minCompressSize, CipherManager cipherManager) {
    byte[] buffer = convertToByteArray(queuedMessages);
    byte[] resultant = compress(buffer, minCompressSize);

    boolean compressed = resultant != buffer; // it will be the same object or not

    // Encrypt post zip
    if(cipherManager != null) {
      resultant = cipherManager.encrypt(resultant);
    }
    return new Packed(resultant, compressed);
  }

  private static byte[] compress(byte[] buffer, int minCompressSize) {
    if (buffer.length >= minCompressSize) {
      try {
        byte[] compressedFrame = deflate(buffer);
        if (buffer.length > compressedFrame.length) {
          buffer = compressedFrame;
        }
      } catch (IOException e) {
        // ignore
      }
    }
    return buffer;
  }

  private static byte[] convertToByteArray(Map<String, List<byte[]>> queuedMessages) {
    // Ensure deterministic order by namespace
    Map<String, List<byte[]>> orderedByNamespace = new TreeMap<>(Comparator.nullsFirst(String::compareTo));
    orderedByNamespace.putAll(queuedMessages);

    List<byte[]> namespaceStreams = convertToStreams(orderedByNamespace);

    int numberOfNamespaces = namespaceStreams.size();
    requireU16(numberOfNamespaces, "number of namespaces");

    int totalFrameLength = 2; // numberOfNamespaces field
    for (byte[] namespaceStream : namespaceStreams) {
      requireU16(namespaceStream.length, "namespace stream length");
      totalFrameLength += 2 + namespaceStream.length;
    }

    ByteBuffer frameBuffer = ByteBuffer.allocate(totalFrameLength);
    frameBuffer.putShort((short) numberOfNamespaces);
    for (byte[] namespaceStream : namespaceStreams) {
      frameBuffer.putShort((short) namespaceStream.length);
      frameBuffer.put(namespaceStream);
    }
    return frameBuffer.array();
  }

  private static List<byte[]> convertToStreams(Map<String, List<byte[]>> queuedMessages) {
    List<byte[]> namespaceStreams = new ArrayList<>(queuedMessages.size());
    for (Map.Entry<String, List<byte[]>> entry : queuedMessages.entrySet()) {
      String namespaceString = entry.getKey() == null ? "" : entry.getKey();
      byte[] namespaceBytes = namespaceString.getBytes(StandardCharsets.UTF_8);
      requireU16(namespaceBytes.length, "namespace length");

      List<byte[]> messagesForNamespace = entry.getValue() == null ? List.of() : entry.getValue();
      requireU16(messagesForNamespace.size(), "message count for namespace: " + namespaceString);

      namespaceStreams.add(packNamespaceStream(namespaceBytes, messagesForNamespace));
    }
    return namespaceStreams;
  }

  private static byte[] packNamespaceStream(byte[] namespaceBytes, List<byte[]> messagesForNamespace) {
    int streamLength = 2 + namespaceBytes.length + 2; // namespace length + namespace + message count
    for (byte[] messageBytes : messagesForNamespace) {
      int messageLength = (messageBytes == null) ? 0 : messageBytes.length;
      requireU16(messageLength, "message length");
      streamLength += 2 + messageLength;
    }
    requireU16(streamLength, "namespace stream total length");

    ByteBuffer namespaceStreamBuffer = ByteBuffer.allocate(streamLength);
    namespaceStreamBuffer.putShort((short) namespaceBytes.length);
    namespaceStreamBuffer.put(namespaceBytes);
    namespaceStreamBuffer.putShort((short) messagesForNamespace.size());

    for (byte[] messageBytes : messagesForNamespace) {
      int messageLength = (messageBytes == null) ? 0 : messageBytes.length;
      namespaceStreamBuffer.putShort((short) messageLength);
      if (messageLength > 0) {
        namespaceStreamBuffer.put(messageBytes);
      }
    }
    return namespaceStreamBuffer.array();
  }

  private static byte[] deflate(byte[] data) throws IOException {
    Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
    deflater.setInput(data);
    deflater.finish();

    byte[] workingBuffer = new byte[8192];
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
      while (!deflater.finished()) {
        int bytesCompressed = deflater.deflate(workingBuffer);
        if (bytesCompressed <= 0) break;
        outputStream.write(workingBuffer, 0, bytesCompressed);
      }
      return outputStream.toByteArray();
    } finally {
      deflater.end();
    }
  }

  private static void requireU16(int value, String description) {
    if (value < 0 || value > MAX_U16) {
      throw new IllegalArgumentException(description + " out of range for u16: " + value);
    }
  }
}
