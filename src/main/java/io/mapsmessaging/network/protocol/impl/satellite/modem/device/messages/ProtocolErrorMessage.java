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

import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.values.ErrorCode;
import lombok.Getter;

@Getter
public class ProtocolErrorMessage implements ModemMessage {

  private final int referenceNumber; // 11 bits
  private final ErrorCode errorCode;       // 8 bits
  private final int errorInfo;       // 8 bits

  public ProtocolErrorMessage(byte[] data) {
    if (data.length < 4) {
      throw new IllegalArgumentException("Invalid MIN001 payload length");
    }

    int header = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    this.referenceNumber = header & 0x07FF; // lower 11 bits
    this.errorCode = ErrorCode.from(data[2] & 0xFF);
    this.errorInfo = data[3] & 0xFF;
  }

}
