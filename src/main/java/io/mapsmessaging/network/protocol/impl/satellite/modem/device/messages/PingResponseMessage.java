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
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.ByteBuffer;

@Getter
@Setter
@NoArgsConstructor
public class PingResponseMessage implements ModemMessage {

  private int requestTime;   // 16 bits
  private int responseTime;  // 16 bits

  public PingResponseMessage(byte[] data) {
    if (data.length < 4) {
      throw new IllegalArgumentException("Invalid MIN110 payload length");
    }
    ByteBuffer buffer = ByteBuffer.wrap(data);
    requestTime = buffer.getShort() & 0xFFFF;
    responseTime = buffer.getShort() & 0xFFFF;
  }

  public byte[] pack() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.putShort((short) (requestTime & 0xFFFF));
    buffer.putShort((short) (responseTime & 0xFFFF));
    return buffer.array();
  }
}
