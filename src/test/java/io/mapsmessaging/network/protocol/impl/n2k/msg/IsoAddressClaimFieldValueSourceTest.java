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

package io.mapsmessaging.network.protocol.impl.n2k.msg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IsoAddressClaimFieldValueSourceTest {

  @Test
  void blankNameUsesDefaultIdentityAndFixedDeviceMetadata() {
    IsoAddressClaimFieldValueSource source = new IsoAddressClaimFieldValueSource(" ");

    assertEquals(60928, IsoAddressClaimFieldValueSource.PGN);
    assertEquals(1L, source.getLong("uniqueNumber"));
    assertEquals(1850L, source.getLong("manufacturerCode"));
    assertEquals(130L, source.getLong("deviceFunction"));
    assertEquals(25L, source.getLong("deviceClass"));
    assertEquals(4L, source.getLong("industryGroup"));
    assertEquals(1L, source.getLong("arbitraryAddressCapable"));
    assertFalse(source.has("missing"));
  }

  @Test
  void namedIdentityIsStableAndConfinedToTwentyOneBits() {
    String name = "node-alpha";
    long expected = Integer.toUnsignedLong(name.hashCode()) & 0x1FFFFFL;
    if (expected == 0L) {
      expected = 1L;
    }

    IsoAddressClaimFieldValueSource first = new IsoAddressClaimFieldValueSource(name);
    IsoAddressClaimFieldValueSource second = new IsoAddressClaimFieldValueSource(name);

    assertEquals(expected, first.getLong("uniqueNumber"));
    assertEquals(first.getLong("uniqueNumber"), second.getLong("uniqueNumber"));
    assertTrue(first.getLong("uniqueNumber") >= 1L);
    assertTrue(first.getLong("uniqueNumber") <= 0x1FFFFFL);
  }
}
