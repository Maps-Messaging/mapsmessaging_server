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

package io.mapsmessaging.network.protocol.impl.satellite.idp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoEntry {
  public static final int TX_READY = 3;
  public static final int TX_SENDING = 4;
  public static final int TX_COMPLETED = 5;
  public static final int OGX_TX_COMPLETED = 6;

  private final int messageId;
  private final int length;
  private int state;              // 3→4→5
  private int bytesAck;           // 0..length
  private boolean completedSeen;  // true after we’ve emitted TX_COMPLETED once

  public MoEntry(int messageId, int length) {
    this.messageId = messageId;
    this.length = length;
    this.state = TX_READY;
    this.bytesAck = 0;
    this.completedSeen = false;
  }
}


