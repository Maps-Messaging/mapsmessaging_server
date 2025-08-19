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

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class SatelliteMessageFactory {

  private static final Logger logger = LoggerFactory.getLogger(SatelliteMessageFactory.class);

  private SatelliteMessageFactory() {
  }

  public static List<SatelliteMessage> createMessages(String namespace, byte[] payload, int maxMessageSize, int minCompressedMessageSize) {
    List<SatelliteMessage> messages = new ArrayList<>();
    boolean compressed = false;

    if (payload.length > minCompressedMessageSize && minCompressedMessageSize > 0) {
      byte[] payload1 = compress(payload);
      if (payload1.length < payload.length) {
        logger.log(STOGI_COMPRESS_MESSAGE, namespace, payload.length, payload1.length);
        payload = payload1;
        compressed = true;
      }
    }
    int maxSize = maxMessageSize - namespace.length();

    int totalChunks = ((payload.length + namespace.length()) + maxSize - 1) / maxSize;

    for (int offset = 0, chunkIndex = 0; offset < payload.length; offset += maxSize, chunkIndex++) {
      int len = Math.min(maxSize, payload.length - offset);
      byte[] chunk = new byte[len];
      System.arraycopy(payload, offset, chunk, 0, len);
      SatelliteMessage message = new SatelliteMessage(
          namespace,
          chunk,
          totalChunks - 1 - chunkIndex, // count down to 0
          compressed
      );
      messages.add(message);
    }
    if (messages.size() > 1) {
      logger.log(STOGI_SPLIT_MESSAGE, namespace, messages.size());
    }
    return messages;
  }

  public static SatelliteMessage reconstructMessage(List<SatelliteMessage> messages) {
    if(messages.size() == 1 && !messages.get(0).isCompressed()) {
      return messages.get(0);
    }
    String namespace = messages.get(0).getNamespace();
    boolean compressed = messages.get(0).isCompressed();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      for (SatelliteMessage message : messages) {
        baos.write(message.getMessage());
      }
    } catch (IOException e) {
      // Log this, since this is weird!!!
    }
    byte[] recombined = baos.toByteArray();
    if (compressed) {
      recombined = decompress(recombined);
    }
    return new SatelliteMessage(namespace, recombined, 0, false);
  }

  public static byte[] compress(byte[] data) {
    Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
    deflater.setInput(data);
    deflater.finish();
    byte[] buffer = new byte[1024];
    int len;
    try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
      while (!deflater.finished()) {
        len = deflater.deflate(buffer);
        baos.write(buffer, 0, len);
      }
      return baos.toByteArray();
    } catch (java.io.IOException e) {
      logger.log(STOGI_EXCEPTION_PROCESSING_PACKET, e);
    }
    return data; // no compression
  }

  public static byte[] decompress(byte[] compressedData) {
    Inflater inflater = new Inflater();
    inflater.setInput(compressedData);
    byte[] buffer = new byte[1024];
    int len;
    try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
      while (!inflater.finished()) {
        len = inflater.inflate(buffer);
        baos.write(buffer, 0, len);
      }
      return baos.toByteArray();
    } catch (IOException | DataFormatException exception) {
      logger.log(STOGI_EXCEPTION_PROCESSING_PACKET, exception);
    }
    return compressedData;
  }
}
