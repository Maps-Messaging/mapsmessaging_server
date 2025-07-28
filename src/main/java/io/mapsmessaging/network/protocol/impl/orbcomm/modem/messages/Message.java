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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.messages;

import io.mapsmessaging.network.protocol.impl.orbcomm.modem.values.MessageFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
public class Message {
  private String name;
  private byte[] payload;
  private int SIN;
  private int MIN;
  private int priority;
  private MessageFormat format;



  public Message(String line) {
    if (!line.startsWith("AT%MGFG:")) {
      throw new IllegalArgumentException("Invalid line: " + line);
    }

    // strip prefix
    String content = line.substring(line.indexOf(':') + 1).trim();

    // name
    int q1 = content.indexOf('"');
    int q2 = content.indexOf('"', q1 + 1);
    if (q1 < 0 || q2 <= q1) {
      throw new IllegalArgumentException("Missing/invalid name: " + line);
    }
    this.name = content.substring(q1 + 1, q2);

    // meta after name + comma+space
    String metaAndMaybeData = content.substring(q2 + 2);

    // msgNum, priority, sin, state, length, dataFormat
    String[] meta = metaAndMaybeData.split(",", 7);
    if (meta.length < 6) {
      throw new IllegalArgumentException("Incomplete MGFG header: " + line);
    }

    this.priority = Integer.parseInt(meta[1].trim());
    this.SIN = Integer.parseInt(meta[2].trim());
    int dataFormat = Integer.parseInt(meta[5].trim());
    this.format = MessageFormat.fromCode(dataFormat);

    // locate payload
    String dataStr = null;
    if (this.format == MessageFormat.TEXT) {
      // next quoted segment is the payload
      int dq1 = metaAndMaybeData.indexOf('"');
      int dq2 = metaAndMaybeData.lastIndexOf('"');
      if (dq1 >= 0 && dq2 > dq1) {
        dataStr = metaAndMaybeData.substring(dq1 + 1, dq2);
      } else {
        dataStr = "";
      }
    } else {
      // payload is after the 7th comma of the whole line (name consumes 1)
      int idx = findNthComma(content, 7);
      if (idx != -1 && idx + 1 < content.length()) {
        dataStr = content.substring(idx + 1).trim();
      } else {
        dataStr = "";
      }
    }

    this.payload = (dataStr.isEmpty()) ? new byte[0] : this.format.decode(dataStr);

    // MIN is not in the header; leave unset or extract from payload if you prepend it yourself
    this.MIN = -1;
  }

  public String toATCommand() {
    String encodedPayload = format.encode(payload);
    return String.format(
        "\"%s\",%d,%d,%d,%d,%s",
        name, priority, SIN, MIN, format.getCode(), encodedPayload
    );
  }


  private static int findNthComma(String s, int n) {
    int pos = -1;
    while (n-- > 0) {
      pos = s.indexOf(',', pos + 1);
      if (pos == -1) return -1;
    }
    return pos;
  }

}