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

import io.mapsmessaging.network.protocol.impl.orbcomm.modem.messages.values.ModemWakeupPeriod;
import lombok.Getter;

import java.nio.ByteBuffer;

@Getter
public class SleepScheduleMessage implements ModemMessage {

  private final ModemWakeupPeriod wakeupType;
  private final boolean mobileInitiated;
  private int messageReference;


  public SleepScheduleMessage(byte[] data) {
    if (data.length < 4) {
      throw new IllegalArgumentException("Invalid MIN070 payload length");
    }
    ByteBuffer buffer = ByteBuffer.wrap(data);
    wakeupType = ModemWakeupPeriod.fromCode(buffer.get() & 0xFF);
    int tmp = buffer.getShort() & 0xFFFF;
    mobileInitiated = (tmp & 0x80) != 0;
    messageReference = (tmp>>4) & 0x7FF;
  }
}
