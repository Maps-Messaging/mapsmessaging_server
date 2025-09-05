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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data;


public enum OperationMode {
  ALWAYS_ON(0),
  WAKE_UP(1),
  RECEIVE_ON_SEND(2),
  HYBRID(3);

  private final int code;

  OperationMode(int code) {
    this.code = code;
  }

  public static OperationMode fromCode(Integer code) {
    if (code == null) return null;
    for (OperationMode mode : values()) {
      if (mode.code == code) return mode;
    }
    return null;
  }

  public int getCode() {
    return code;
  }
}