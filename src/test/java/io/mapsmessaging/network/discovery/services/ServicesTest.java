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

package io.mapsmessaging.network.discovery.services;

import io.mapsmessaging.network.discovery.MapsServiceInfo;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicesTest {

  @Test
  void constructorCopiesProtocolTransportAddressesAndProperties() {
    MapsServiceInfo info = service(new String[]{"10.0.0.1", "10.0.0.2"});

    Services services = new Services(info);

    assertEquals("mqtt", services.getProtocol());
    assertEquals(1883, services.getPort());
    assertEquals("tcp", services.getTransport());
    assertEquals(java.util.List.of("10.0.0.1", "10.0.0.2"), services.getAddresses());
    assertEquals(Map.of("role", "broker"), services.getProperties());
  }

  @Test
  void mergeAddsOnlyNewAddresses() {
    Services services = new Services(service(new String[]{"10.0.0.1"}));

    services.mergeServices(service(new String[]{"10.0.0.1", "10.0.0.2"}));

    assertEquals(java.util.List.of("10.0.0.1", "10.0.0.2"), services.getAddresses());
  }

  @Test
  void stringRenderingContainsEndpointAddressesAndProperties() {
    Services services = new Services(service(new String[]{"10.0.0.1"}));

    String text = services.toString();

    assertTrue(text.contains("mqtt:1883"));
    assertTrue(text.contains("[10.0.0.1]"));
    assertTrue(text.contains("[role=broker]"));
  }

  private static MapsServiceInfo service(String[] addresses) {
    MapsServiceInfo info = mock(MapsServiceInfo.class);
    when(info.getApplication()).thenReturn("mqtt");
    when(info.getPort()).thenReturn(1883);
    when(info.getProtocol()).thenReturn("tcp");
    when(info.getHostAddresses()).thenReturn(addresses);
    when(info.getProperties()).thenReturn(Map.of("role", "broker"));
    return info;
  }
}
