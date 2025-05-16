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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import lombok.Getter;

public enum ReasonCodes {

  SUCCESS(0x0),
  CONGESTION(0x1),
  INVALID_TOPIC_ALIAS(0x2),
  NOT_SUPPORTED(0x3),
  NO_SESSION(0x5),
  CONTINUE_AUTHENTICATION(0x18),
  RE_AUTHENTICATE(0x19),
  BAD_AUTH(0x8C),
  NO_AUTH(0x87),
  PACKET_TOO_LARGE(0x95),
  PAYLOAD_FORMAT_INVALID(0x99),
  UNSUPPORTED(0x84);

  @Getter
  private final int value;

  ReasonCodes(int val) {
    value = val;
  }

  public static ReasonCodes lookup(int val) {
    switch (val) {
      case 0x0:
        return SUCCESS;
      case 0x1:
        return CONGESTION;
      case 0x2:
        return INVALID_TOPIC_ALIAS;
      case 0x3:
        return NOT_SUPPORTED;
      case 0x5:
        return NO_SESSION;
      case 0x18:
        return CONTINUE_AUTHENTICATION;
      case 0x19:
        return RE_AUTHENTICATE;
      case 0x8C:
        return BAD_AUTH;
      case 0x87:
        return NO_AUTH;
      case 0x95:
        return PACKET_TOO_LARGE;
      case 0x99:
        return PAYLOAD_FORMAT_INVALID;
      case 0x84:
        return UNSUPPORTED;
      default:
        return UNSUPPORTED;
    }
  }
}

