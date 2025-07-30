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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages;

import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.values.ModemProductId;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.values.ModemResetReason;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.values.ModemWakeupPeriod;
import lombok.Getter;

import java.nio.ByteBuffer;

@Getter
public class ModemIdentificationMessage implements ModemMessage {

  private final int hardwareMajor;
  private final int hardwareMinor;
  private final int softwareMajor;
  private final int softwareMinor;
  private final ModemProductId productId;
  private final ModemWakeupPeriod wakeupType;
  private final ModemResetReason lastReset;
  private final int virtualCarrier;
  private final int bean;
  private final int vain;
  private final int reservedBits;
  private final int operatorTxState;
  private final int userTxState;
  private final int broadcastId;

  public ModemIdentificationMessage(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);

    int hwVersion = buffer.getShort() & 0xFFFF;
    hardwareMajor = (hwVersion >> 8) & 0xFF;
    hardwareMinor = hwVersion & 0xFF;

    int swVersion = buffer.getShort() & 0xFFFF;
    softwareMajor = (swVersion >> 8) & 0xFF;
    softwareMinor = swVersion & 0xFF;

    productId = ModemProductId.fromId(buffer.get() & 0xFF);
    wakeupType = ModemWakeupPeriod.fromCode(buffer.get() & 0xFF);
    lastReset = ModemResetReason.fromValue(buffer.get() & 0xFF);

    int combined = ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);
    virtualCarrier = (combined >> 4) & 0x0FFF;
    bean = combined & 0x0F;

    vain = buffer.getShort() & 0xFFFF;

    int flags = buffer.get() & 0xFF;
    reservedBits = (flags >> 6) & 0x03;
    operatorTxState = (flags >> 3) & 0x07;
    userTxState = flags & 0x07;

    broadcastId = buffer.get() & 0xFF;
  }
}