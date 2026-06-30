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

package io.mapsmessaging.api.features;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FeatureBehaviorTest {

  @Test
  void priority_getInstance_clampsValuesOutsideSupportedRange() {
    assertSame(Priority.LOWEST, Priority.getInstance(-1));
    assertSame(Priority.LOWEST, Priority.getInstance(Integer.MIN_VALUE));
    assertSame(Priority.HIGHEST, Priority.getInstance(11));
    assertSame(Priority.HIGHEST, Priority.getInstance(Integer.MAX_VALUE));
  }

  @Test
  void priority_getInstance_mapsEverySupportedValue() {
    for (Priority priority : Priority.values()) {
      assertSame(priority, Priority.getInstance(priority.getValue()));
    }
  }

  @Test
  void numericFeatureFactories_rejectUnsupportedValues() {
    assertThrows(IllegalArgumentException.class, () -> ClientAcknowledgement.getInstance(-1));
    assertThrows(IllegalArgumentException.class, () -> CreditHandler.getInstance(2));
    assertThrows(IllegalArgumentException.class, () -> DestinationMode.getInstance(2));
    assertThrows(IllegalArgumentException.class, () -> QualityOfService.getInstance(4));
    assertThrows(IllegalArgumentException.class, () -> RetainHandler.getInstance(3));
  }

  @Test
  void destinationType_getType_isCaseInsensitiveAndRejectsUnknownTypes() {
    assertSame(DestinationType.TOPIC, DestinationType.getType("ToPiC"));
    assertSame(DestinationType.TEMPORARY_QUEUE, DestinationType.getType("temporaryqueue"));
    assertSame(DestinationType.SCHEMA, DestinationType.getType("SCHEMA"));
    assertSame(DestinationType.METRICS, DestinationType.getType("metrics"));
    assertThrows(RuntimeException.class, () -> DestinationType.getType("stream"));
  }

  @Test
  void rollbackPriority_increment_clampsAtHighestPriority() {
    assertEquals(Priority.NORMAL.getValue(), RollbackPriority.MAINTAIN.incrementPriority(Priority.NORMAL.getValue()));
    assertEquals(Priority.ONE_ABOVE_NORMAL.getValue(), RollbackPriority.INCREMENT.incrementPriority(Priority.NORMAL.getValue()));
    assertEquals(Priority.HIGHEST.getValue(), RollbackPriority.INCREMENT.incrementPriority(Priority.HIGHEST.getValue()));
  }

  @Test
  void inflatorCompression_roundTripsPayload() {
    byte[] payload = "sensor/temperature=21.5;".repeat(100).getBytes(StandardCharsets.UTF_8);

    ByteBuffer compressed = CompressionMode.INFLATOR.compress(payload);
    byte[] decompressed = CompressionMode.INFLATOR.decompress(compressed);

    assertArrayEquals(payload, decompressed);
    assertTrue(compressed.remaining() < payload.length);
  }

  @Test
  void noneCompression_preservesPayload() {
    byte[] payload = new byte[]{0, 1, 2, 3, 4};

    ByteBuffer compressed = CompressionMode.NONE.compress(payload);

    assertArrayEquals(payload, compressed.array());
  }
}
