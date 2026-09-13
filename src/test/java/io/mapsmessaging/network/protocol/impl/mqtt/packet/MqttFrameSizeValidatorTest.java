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

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MqttFrameSizeValidatorTest {

  @Test
  void acceptsFrameAtReadBufferLimit() {
    Assertions.assertDoesNotThrow(() -> MqttFrameSizeValidator.validate(100 * 1024L, 100 * 1024L));
  }

  @Test
  void rejectsFrameAboveReadBufferLimitWithDiagnosticSizes() {
    MalformedException exception = Assertions.assertThrows(
        MalformedException.class,
        () -> MqttFrameSizeValidator.validate(300 * 1024L, 100 * 1024L));

    Assertions.assertTrue(exception.getMessage().contains("307200"));
    Assertions.assertTrue(exception.getMessage().contains("102400"));
    Assertions.assertTrue(exception.getMessage().contains("serverReadBufferSize"));
    Assertions.assertTrue(exception.getMessage().contains("closing connection"));
  }
}
