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

package io.mapsmessaging.network.io.impl.tcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkHelperTest {

  @Test
  void ipv4AddressInsideCidrReturnsTrue() {
    assertTrue(NetworkHelper.isInCidr("192.168.10.0/24", "192.168.10.42"));
  }

  @Test
  void ipv4AddressOutsideCidrReturnsFalse() {
    assertFalse(NetworkHelper.isInCidr("192.168.10.0/24", "192.168.11.42"));
  }

  @Test
  void ipv6AddressInsideCidrReturnsTrue() {
    assertTrue(NetworkHelper.isInCidr("2001:db8::/32", "2001:db8:1::1"));
  }

  @Test
  void addressFamilyMismatchReturnsFalse() {
    assertFalse(NetworkHelper.isInCidr("192.168.10.0/24", "2001:db8::1"));
  }

  @Test
  void malformedCidrReturnsFalse() {
    assertFalse(NetworkHelper.isInCidr("not-a-cidr", "192.168.10.42"));
    assertFalse(NetworkHelper.isInCidr("192.168.10.0/not-a-prefix", "192.168.10.42"));
  }

  @Test
  void negativePrefixReturnsFalse() {
    assertFalse(NetworkHelper.isInCidr("10.0.0.0/-1", "203.0.113.7"));
  }

  @Test
  void prefixLongerThanAddressReturnsFalse() {
    assertFalse(NetworkHelper.isInCidr("10.0.0.0/33", "10.0.0.1"));
    assertFalse(NetworkHelper.isInCidr("2001:db8::/129", "2001:db8::1"));
  }

  @Test
  void zeroPrefixMatchesAnyAddressOfSameFamily() {
    assertTrue(NetworkHelper.isInCidr("10.0.0.0/0", "203.0.113.7"));
    assertTrue(NetworkHelper.isInCidr("2001:db8::/0", "fd00::1"));
  }

  @Test
  void hostPrefixMatchesOnlyExactAddress() {
    assertTrue(NetworkHelper.isInCidr("192.168.10.42/32", "192.168.10.42"));
    assertFalse(NetworkHelper.isInCidr("192.168.10.42/32", "192.168.10.43"));
  }
}
