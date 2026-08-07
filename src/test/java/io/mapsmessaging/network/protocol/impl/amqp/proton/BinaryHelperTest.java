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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import org.apache.qpid.proton.amqp.Binary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BinaryHelperTest {

  @Test
  void copy_with_sliced_binary_returns_only_visible_bytes() {
    Binary binary = new Binary(new byte[]{1, 2, 3, 4, 5}, 1, 3);

    assertArrayEquals(new byte[]{2, 3, 4}, BinaryHelper.copy(binary));
  }

  @Test
  void copy_with_null_returns_null() {
    assertNull(BinaryHelper.copy(null));
  }
}
