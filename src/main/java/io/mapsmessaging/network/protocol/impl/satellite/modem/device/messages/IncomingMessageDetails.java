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
  private int bytes;
  private int bytesReceived;
  private String dateTime;

  public IncomingMessageDetails(String line, boolean isOgx) {

    String[] parts = line.split(",");
    if (isOgx) {
      id = parts[1];
      messageId = Float.NaN;
      sin = -1;
      min = Integer.parseInt(parts[0]);
      dateTime = parts[2];
      bytes = Integer.parseInt(parts[5]);  // Same
      bytesReceived = bytes;
    } else {
      id = parts[0];
      messageId = Float.parseFloat(parts[1]);
      sin = Integer.parseInt(parts[2]);
      min = Integer.parseInt(parts[3]);
      bytes = Integer.parseInt(parts[4]);
      bytesReceived = Integer.parseInt(parts[5]);
      dateTime = "";
    }
  }
}
