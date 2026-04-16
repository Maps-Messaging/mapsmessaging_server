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

  private static final long UNIQUE_NUMBER_MASK = 0x1FFFFFL;
  private static final long DEFAULT_IDENTITY_NUMBER = 1L;

  public IsoAddressClaimFieldValueSource(String name) {
    putLong("name", buildName(name));
  }

  private long buildName(String name) {
    long arbitraryAddressCapable = 1L;
    long industryGroup = 4L;
    long vehicleSystemInstance = 0L;
    long systemInstance = 0L;
    long function = 130L;
    long functionInstance = 0L;
    long ecuInstance = 0L;
    long manufacturerCode = 2047L;
    long identityNumber = resolveIdentityNumber(name);

    long value = 0L;
    value |= (identityNumber & UNIQUE_NUMBER_MASK);
    value |= (manufacturerCode & 0x7FFL) << 21;
    value |= (ecuInstance & 0x7L) << 32;
    value |= (functionInstance & 0x1FL) << 35;
    value |= (function & 0xFFL) << 40;
    value |= (vehicleSystemInstance & 0x0FL) << 49;
    value |= (systemInstance & 0x0FL) << 53;
    value |= (industryGroup & 0x07L) << 60;
    value |= (arbitraryAddressCapable & 0x01L) << 63;
    return value;
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