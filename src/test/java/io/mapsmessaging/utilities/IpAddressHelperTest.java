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

package io.mapsmessaging.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpAddressHelperTest {

  @Test
  void normalizeIp_returnsUnknownForMissingAddress() {
    assertEquals("unknown", IpAddressHelper.normalizeIp(null));
    assertEquals("unknown", IpAddressHelper.normalizeIp("  "));
  }

  @Test
  void normalizeIp_trimsAndRemovesLeadingSlash() {
    assertEquals("192.0.2.10", IpAddressHelper.normalizeIp(" /192.0.2.10 "));
    assertEquals("2001:db8::1", IpAddressHelper.normalizeIp("/2001:db8::1"));
  }

  @Test
  void normalizeIp_removesIpv4Port() {
    assertEquals("192.0.2.10", IpAddressHelper.normalizeIp("192.0.2.10:1883"));
    assertEquals("192.0.2.10", IpAddressHelper.normalizeIp("/192.0.2.10:1883"));
  }

  @Test
  void normalizeIp_removesBracketsAndPortFromIpv6() {
    assertEquals("2001:db8::1", IpAddressHelper.normalizeIp("[2001:db8::1]:1883"));
    assertEquals("::1", IpAddressHelper.normalizeIp("/[::1]:1883"));
  }

  @Test
  void normalizeIp_preservesUnbracketedIpv6() {
    assertEquals("2001:db8::1", IpAddressHelper.normalizeIp("2001:db8::1"));
  }
}
