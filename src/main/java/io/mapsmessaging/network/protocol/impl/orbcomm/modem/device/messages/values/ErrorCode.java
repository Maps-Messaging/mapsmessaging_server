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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.values;

public enum ErrorCode {
  UNABLE_TO_ALLOCATE_MESSAGE_BUFFER(1),
  UNKNOWN_MESSAGE_TYPE(2);

  private final int code;

  ErrorCode(int code) {
    this.code = code;
  }

  public static ErrorCode from(int code) {
    for (ErrorCode e : values()) {
      if (e.code == code) return e;
    }
    return null;
  }

  public int getCode() {
    return code;
  }
}

