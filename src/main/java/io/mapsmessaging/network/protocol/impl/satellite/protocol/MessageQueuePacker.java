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

import io.mapsmessaging.network.protocol.impl.satellite.modem.xmodem.Xmodem;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.Deflater;

public final class MessageQueuePacker extends MessageQueue{
  private MessageQueuePacker() {}

  public record Packed(byte[] data, boolean compressed, int transformerNumber) {}

  public static Packed pack(Map<String, List<byte[]>> queuedMessages, int minCompressSize, CipherManager cipherManager, ProtocolMessageTransformation transformer) throws IOException {
    MessageQueuePacker packer = new MessageQueuePacker();
    byte[] buffer = packer.convertToByteArray(queuedMessages);
    byte[] resultant = compress(buffer, minCompressSize);

    boolean compressed = resultant != buffer; // it will be the same object or not

    // Encrypt post zip
    if(cipherManager != null) {
      resultant = cipherManager.encrypt(resultant);
    }
    int id = transformer == null ? 0 : transformer.getId();
    return new Packed(resultant, compressed, id);
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

  private byte[] convertToByteArray(Map<String, List<byte[]>> queuedMessages) {
    // Ensure deterministic order by namespace
    Map<String, List<byte[]>> orderedByNamespace = new TreeMap<>(Comparator.nullsFirst(String::compareTo));
    orderedByNamespace.putAll(queuedMessages);

    List<byte[]> namespaceStreams = convertToStreams(orderedByNamespace);

    int numberOfNamespaces = namespaceStreams.size();
    requireUnsignedInRange(numberOfNamespaces, LENGTH_BYTE_SIZE,"number of namespaces");

    int totalFrameLength = LENGTH_BYTE_SIZE; // numberOfNamespaces field
    for (byte[] namespaceStream : namespaceStreams) {
      requireUnsignedInRange(namespaceStream.length, LENGTH_BYTE_SIZE, "namespace stream length");
      totalFrameLength += LENGTH_BYTE_SIZE + namespaceStream.length;
    }

    ByteBuffer frameBuffer = ByteBuffer.allocate(totalFrameLength+5);
    frameBuffer.put(LENGTH_BYTE_SIZE); // size used for packing/unpacking
    putVarUInt(frameBuffer, numberOfNamespaces, LENGTH_BYTE_SIZE);
    for (byte[] namespaceStream : namespaceStreams) {
      putVarUInt(frameBuffer, namespaceStream.length, LENGTH_BYTE_SIZE);
      frameBuffer.put(namespaceStream);
    }
    byte[] frame = frameBuffer.array();
    int crc= (int)(Xmodem.crc32Mpeg2(frame, frame.length-4) & 0xFFFFFFFF);
    frameBuffer.putInt(crc);
    return frameBuffer.array();
  }

  private List<byte[]> convertToStreams(Map<String, List<byte[]>> queuedMessages) {
    List<byte[]> namespaceStreams = new ArrayList<>(queuedMessages.size());
    for (Map.Entry<String, List<byte[]>> entry : queuedMessages.entrySet()) {
      String namespaceString = entry.getKey() == null ? "" : entry.getKey();
      byte[] namespaceBytes = namespaceString.getBytes(StandardCharsets.UTF_8);
      requireUnsignedInRange(namespaceBytes.length,  LENGTH_BYTE_SIZE,"namespace length");

      List<byte[]> messagesForNamespace = entry.getValue() == null ? List.of() : entry.getValue();
      requireUnsignedInRange(messagesForNamespace.size(),  LENGTH_BYTE_SIZE,"message count for namespace: " + namespaceString);

      namespaceStreams.add(packNamespaceStream(namespaceBytes, messagesForNamespace));
    }
    return namespaceStreams;
  }

  private byte[] packNamespaceStream(byte[] namespaceBytes, List<byte[]> messagesForNamespace) {
    int streamLength = LENGTH_BYTE_SIZE + namespaceBytes.length + LENGTH_BYTE_SIZE; // namespace length + namespace + message count
    for (byte[] messageBytes : messagesForNamespace) {
      int messageLength = (messageBytes == null) ? 0 : messageBytes.length;
      requireUnsignedInRange(messageLength,  LENGTH_BYTE_SIZE,"message length");
      streamLength += LENGTH_BYTE_SIZE + messageLength;
    }
    requireUnsignedInRange(streamLength,  LENGTH_BYTE_SIZE,"namespace stream total length");

    ByteBuffer namespaceStreamBuffer = ByteBuffer.allocate(streamLength);
    putVarUInt(namespaceStreamBuffer,namespaceBytes.length, LENGTH_BYTE_SIZE);
    namespaceStreamBuffer.put(namespaceBytes);
    putVarUInt(namespaceStreamBuffer, messagesForNamespace.size(),LENGTH_BYTE_SIZE);
    for (byte[] messageBytes : messagesForNamespace) {
      int messageLength = (messageBytes == null) ? 0 : messageBytes.length;
      putVarUInt(namespaceStreamBuffer, messageLength, LENGTH_BYTE_SIZE);
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

}
