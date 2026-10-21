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

package io.mapsmessaging;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageDaemonConsulMetadataTest {

  @Test
  void buildRestEndpoint_usesConfiguredAddress() throws Exception {
    String endpoint = MessageDaemon.buildRestEndpoint("192.0.2.20", 8081, InetAddress.getByName("192.0.2.10"));

    assertEquals("192.0.2.20:8081", endpoint);
  }

  @Test
  void buildRestEndpoint_replacesIpv4WildcardWithLocalAddress() throws Exception {
    String endpoint = MessageDaemon.buildRestEndpoint("0.0.0.0", 8080, InetAddress.getByName("192.0.2.10"));

    assertEquals("192.0.2.10:8080", endpoint);
  }

  @Test
  void buildRestEndpoint_replacesWildcardListWithLocalAddress() throws Exception {
    String endpoint = MessageDaemon.buildRestEndpoint("0.0.0.0, ::", 8080, InetAddress.getByName("192.0.2.10"));

    assertEquals("192.0.2.10:8080", endpoint);
  }

  @Test
  void buildRestEndpoint_formatsIpv6Address() throws Exception {
    String endpoint = MessageDaemon.buildRestEndpoint("::", 8443, InetAddress.getByName("2001:db8::10"));

    assertEquals("[2001:db8:0:0:0:0:0:10]:8443", endpoint);
  }

  @Test
  void buildRestEndpoint_rejectsInvalidPort() throws Exception {
    InetAddress address = InetAddress.getByName("192.0.2.10");

    assertThrows(IllegalArgumentException.class, () -> MessageDaemon.buildRestEndpoint("0.0.0.0", 0, address));
    assertThrows(IllegalArgumentException.class, () -> MessageDaemon.buildRestEndpoint("0.0.0.0", 65536, address));
  }
}
