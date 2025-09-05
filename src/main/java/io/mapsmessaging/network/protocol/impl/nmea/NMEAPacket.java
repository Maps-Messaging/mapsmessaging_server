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

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class NMEAPacket {

  @Getter
  private final String sentence;
  @Getter
  private final String name;

  private final List<String> entries;

  public NMEAPacket(Packet packet) throws IOException {
    int pos = packet.position();
    int startPos = skipToStart(packet);
    if (startPos != -1) {
      try {
        sentence = parseSentence(packet);
        entries = new ArrayList<>(Arrays.asList(sentence.split(",")));
        name = entries.remove(0);
      } catch (EndOfBufferException e) {
        packet.position(pos);
        throw e;
      }
    } else {
      name = "";
      sentence = "";
      entries = new ArrayList<>();
    }
  }

  public NMEAPacket(String sentence) {
    this.sentence = sentence;
    entries = new ArrayList<>(Arrays.asList(sentence.split(",")));
    name = entries.remove(0);
  }


  public Iterator<String> getEntries() {
    return entries.iterator();
  }

  private int skipToStart(Packet packet) {
    int startPos = -1;
    while (packet.hasRemaining() && startPos == -1) { // Read and ignore to Start char
      byte val = packet.get();
      if (val == Constants.START) {
        startPos = packet.position();
      }
    }
    return startPos;
  }

  private String parseSentence(Packet packet) throws IOException {
    int checksum = 0;
    StringBuilder sentenceBuilder = new StringBuilder();
    while (packet.hasRemaining()) {
      byte val = packet.get();
      if (val == Constants.CHECKSUM) {
        // Now read to eol
        val = packet.get();
        StringBuilder sb = new StringBuilder();
        while (val != Constants.CR && val != Constants.LF && packet.hasRemaining()) {
          sb.append((char) val);
          val = packet.get();
        }
        packet.get(); // Eat the last char
        int sentCheckSum = Integer.parseInt(sb.toString(), 16);
        if (sentCheckSum == checksum) {
          return sentenceBuilder.toString();
        } else {
          throw new IOException("Invalid Checksum calculated");
        }
      } else {
        sentenceBuilder.append((char) val);
        checksum ^= val;
      }
      if (!packet.hasRemaining()) {
        throw new EndOfBufferException();
      }
    }
    throw new EndOfBufferException();
  }

}
