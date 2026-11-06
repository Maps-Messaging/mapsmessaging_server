/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusCodeTest {

  @ParameterizedTest
  @EnumSource(StatusCode.class)
  void byteValueRoundTripsToSameStatus(StatusCode statusCode) {
    assertSame(statusCode, StatusCode.getInstance(statusCode.getValue()));
    assertNotNull(statusCode.getDescription());
    assertFalse(statusCode.getDescription().isBlank());
  }

  @Test
  void unknownStatusCodeIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> StatusCode.getInstance((byte) 0xFF));
  }
}
