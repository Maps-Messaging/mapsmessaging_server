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

package io.mapsmessaging.api.features;

import lombok.Getter;
import lombok.ToString;

/**
 * Enum that indicates what to do with a retained event on a new subscription
 */
@ToString
public enum RetainHandler {

  SEND_ALWAYS(
      0,
      "Send any retained messages that match the subscription"
  ),
  SEND_IF_NEW(
      1,
      "Only send retained messages IF the subscription did not previously exist"
  ),
  DO_NOT_SEND(
      2,
      "Do not send ANY retained messages, regardless of subscription state"
  );

  @Getter
  private final int handler;
  @Getter
  private final String description;

  RetainHandler(int handler, String description) {
    this.handler = handler;
    this.description = description;
  }

  public static RetainHandler getInstance(int value) {
    switch (value) {
      case 0:
        return SEND_ALWAYS;
      case 1:
        return SEND_IF_NEW;
      case 2:
        return DO_NOT_SEND;

      default:
        throw new IllegalArgumentException("Invalid handler value supplied");
    }
  }

}
