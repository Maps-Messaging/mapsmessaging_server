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

package io.mapsmessaging.network.protocol.impl.satellite.modem.idp;

import lombok.Getter;
import lombok.Setter;

import static io.mapsmessaging.network.protocol.impl.satellite.modem.idp.Constants.RX_COMPLETE;

@Getter
@Setter
public class IdpFwdEntry {

  private final int msgNum;
  private final String name;   // "fwdMsgName"
  private final int priority;  // mock (e.g., 4)
  private final int sin;       // mock (e.g., 128)
  private final int length;
  private int state;
  private final byte[] data;

  public IdpFwdEntry(int msgNum, String name, int priority, int sin, byte[] data) {
    this.msgNum = msgNum;
    this.name = name;
    this.priority = priority;
    this.state = RX_COMPLETE;
    this.sin = sin;
    this.length = data.length;
    this.data = data;
  }
}
