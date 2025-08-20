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

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class SatelliteMessageFactory {

  private static final Logger logger = LoggerFactory.getLogger(SatelliteMessageFactory.class);

  private SatelliteMessageFactory() {
  }
  public static List<SatelliteMessage> createMessages(int streamId, byte[] payload, int maxMessageSize, boolean compressed) {
    List<SatelliteMessage> messages = new ArrayList<>();

    int totalChunks = (payload.length + maxMessageSize - 1) / maxMessageSize;

    for (int offset = 0, chunkIndex = 0; offset < payload.length; offset += maxMessageSize, chunkIndex++) {
      int len = Math.min(maxMessageSize, payload.length - offset);
      byte[] chunk = new byte[len];
      System.arraycopy(payload, offset, chunk, 0, len);
      SatelliteMessage message = new SatelliteMessage(
          streamId,
          chunk,
          totalChunks - 1 - chunkIndex, // count down to 0
          compressed
      );
      messages.add(message);
    }
    if (messages.size() > 1) {
      logger.log(STOGI_SPLIT_MESSAGE,  messages.size());
    }
    return messages;
  }

  public static SatelliteMessage reconstructMessage(List<SatelliteMessage> messages) {
    if(messages.size() == 1 && !messages.get(0).isCompressed()) {
      return messages.get(0);
    }
    int streamId = messages.get(0).getStreamNumber();
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
    return new SatelliteMessage(streamId, recombined, 0, compressed);
  }

}
