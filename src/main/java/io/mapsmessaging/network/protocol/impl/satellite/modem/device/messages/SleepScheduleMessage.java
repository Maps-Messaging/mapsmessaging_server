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

import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.values.ModemWakeupPeriod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.ByteBuffer;

@Getter
@Setter
@NoArgsConstructor
public class SleepScheduleMessage implements ModemMessage {

  private ModemWakeupPeriod wakeupType;
  private boolean mobileInitiated;
  private int messageReference;


  public SleepScheduleMessage(byte[] data) {
    if (data.length < 4) {
      throw new IllegalArgumentException("Invalid MIN070 payload length");
    }
    ByteBuffer buffer = ByteBuffer.wrap(data);
    wakeupType = ModemWakeupPeriod.fromCode(buffer.get() & 0xFF);
    int tmp = buffer.getShort() & 0xFFFF;
    mobileInitiated = (tmp & 0x80) != 0;
    messageReference = (tmp >> 4) & 0x7FF;
  }

  public byte[] pack() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.put((byte) (wakeupType != null ? wakeupType.getCode() & 0xFF : 0));
    int tmp = ((messageReference & 0x7FF) << 4) | (mobileInitiated ? 0x80 : 0x00);
    buffer.putShort((short) (tmp & 0xFFFF));
    buffer.put((byte) 0); // Reserved or unused final byte to maintain 4-byte length
    return buffer.array();
  }
}
