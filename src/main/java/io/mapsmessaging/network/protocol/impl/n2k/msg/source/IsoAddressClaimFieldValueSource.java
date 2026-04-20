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

package io.mapsmessaging.network.protocol.impl.n2k.msg.source;

public class IsoAddressClaimFieldValueSource extends AbstractAisFieldValueSource {

  public static final int PGN = 60928;

  private static final long UNIQUE_NUMBER_MASK = 0x1FFFFFL;
  private static final long DEFAULT_IDENTITY_NUMBER = 1L;

  public IsoAddressClaimFieldValueSource(String name) {
    long identityNumber = resolveIdentityNumber(name);

    putLong("uniqueNumber", identityNumber);
    putLong("manufacturerCode", 2047L);
    putLong("deviceInstanceLower", 0L);
    putLong("deviceInstanceUpper", 0L);
    putLong("deviceFunction", 130L);
    putLong("reserved", 0L);
    putLong("deviceClass", 25L);
    putLong("systemInstance", 0L);
    putLong("industryGroup", 4L);
    putLong("arbitraryAddressCapable", 1L);
  }

  private long resolveIdentityNumber(String name) {
    if (name == null || name.isBlank()) {
      return DEFAULT_IDENTITY_NUMBER;
    }

    long identityNumber = Integer.toUnsignedLong(name.hashCode()) & UNIQUE_NUMBER_MASK;
    if (identityNumber == 0L) {
      return DEFAULT_IDENTITY_NUMBER;
    }
    return identityNumber;
  }
}