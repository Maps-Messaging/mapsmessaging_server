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

package io.mapsmessaging.network.protocol.impl.satellite.modem.ogx;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OgxEntry  {
  final int msgId;
  final int type;            // 1 if len <= 1024, else 2
  final String timestamp;    // "yyyy-MM-dd HH:mm:ss" UTC
  final int length;
  final byte[] data;
  int state = 5; // simple model: fixed 4 until delete
  int closed = 0;                // 0 open, 1 final (flip to 1 when you want)

  OgxEntry(int msgId, byte[] data) {
    this.msgId = msgId;
    this.length = data.length;
    this.type = (length <= 1024) ? 1 : 2;
    this.timestamp = OgxModemRegistation.OGX_UTC_FMT.format(java.time.Instant.now());
    this.data = data;
  }
}