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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class IncomingMessageDetails {
  private String id;
  private float messageId;
  private int sin;
  private int min;
  private int priority;
  private int bytes;
  private int bytesReceived;
  private int state;
  private boolean closed;
  private boolean completed;
  private String dateTime;

  public IncomingMessageDetails(String line, boolean isOgx) {
    String[] parts = line.split(",");
    if (isOgx) {
      messageId = Float.NaN;
      sin = -1;
      min = -1;

      // parts[0] == type 1 < 1024 or 2 >= 1024, we ignore this
      id = parts[1];
      dateTime = parts[2];
      state = Integer.parseInt(parts[3]);
      closed = parts[4].equals("1");
      bytes = Integer.parseInt(parts[5]);
      bytesReceived = bytes;
      completed = state == 5;
    } else {
      id = parts[0];
      messageId = Float.parseFloat(parts[1]);
      priority = Integer.parseInt(parts[2]);
      sin = Integer.parseInt(parts[3]);
      state = Integer.parseInt(parts[4]);
      bytes = Integer.parseInt(parts[5]);
      bytesReceived = Integer.parseInt(parts[6]);
      dateTime = "";
      completed = state == 3;
    }
  }
}
