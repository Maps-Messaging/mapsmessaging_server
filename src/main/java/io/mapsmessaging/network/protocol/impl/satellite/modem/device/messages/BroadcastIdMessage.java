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

import lombok.Getter;

@Getter
public class BroadcastIdMessage implements ModemMessage {

  private final int[] broadcastIds = new int[16]; // 24-bit each

  public BroadcastIdMessage(byte[] data) {
    if (data.length < 48) {
      throw new IllegalArgumentException("Invalid MIN115 payload length");
    }

    for (int i = 0; i < 16; i++) {
      int index = i * 3;
      broadcastIds[i] = ((data[index] & 0xFF) << 16)
          | ((data[index + 1] & 0xFF) << 8)
          | (data[index + 2] & 0xFF);
    }
  }
}
