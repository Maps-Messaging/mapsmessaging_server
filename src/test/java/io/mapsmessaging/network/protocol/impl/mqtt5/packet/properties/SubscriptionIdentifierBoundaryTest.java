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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionIdentifierBoundaryTest {

  @ParameterizedTest
  @CsvSource({
      "0,1",
      "1,1",
      "126,1",
      "127,4",
      "128,2",
      "32638,2",
      "32639,4",
      "32640,3",
      "16777086,3",
      "16777087,4",
      "16777088,4",
      "268435455,4"
  })
  void reportsEncodedSizeAcrossVariableIntegerBoundaries(long value, int expectedSize) {
    assertEquals(expectedSize, new SubscriptionIdentifier(value).getSize());
  }

  @ParameterizedTest
  @CsvSource({
      "0",
      "1",
      "126",
      "127",
      "128",
      "16383",
      "16384",
      "2097151",
      "2097152",
      "268435455"
  })
  void packAndLoadRoundTripsVariableInteger(long value) throws Exception {
    SubscriptionIdentifier original = new SubscriptionIdentifier(value);
    Packet packet = new Packet(ByteBuffer.allocate(8));

    original.pack(packet);
    packet.flip();

    SubscriptionIdentifier restored = (SubscriptionIdentifier) original.instance();
    restored.load(packet);

    assertEquals(value, restored.getSubscriptionIdentifier());
  }
}
