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

package io.mapsmessaging.network.discovery;

import org.junit.jupiter.api.Test;

import javax.jmdns.ServiceInfo;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapsServiceInfoTest {

  @Test
  void delegatesServiceMetadataAndKnownProperties() {
    ServiceInfo service = mock(ServiceInfo.class);
    when(service.getPropertyString("protocol")).thenReturn("mqtt");
    when(service.getPropertyString("schema support")).thenReturn("true");
    when(service.getPropertyString("schema name")).thenReturn("/schema");
    when(service.getPropertyString("date")).thenReturn("2026-09-19");
    when(service.getPropertyString("server name")).thenReturn("server-a");
    when(service.getPropertyString("system topics")).thenReturn("$SYS");
    when(service.getPropertyString("version")).thenReturn("4.5.0");
    when(service.getName()).thenReturn("fallback");
    when(service.getType()).thenReturn("_maps._tcp.local.");
    when(service.getHostAddresses()).thenReturn(new String[]{"10.0.0.1"});
    when(service.getDomain()).thenReturn("local.");
    when(service.getPort()).thenReturn(1883);
    when(service.getApplication()).thenReturn("maps");

    MapsServiceInfo info = new MapsServiceInfo(service);

    assertEquals("mqtt", info.getProtocol());
    assertTrue(info.supportsSchema());
    assertEquals("/schema", info.getSchemaPrefix());
    assertEquals("2026-09-19", info.getBuildDate());
    assertEquals("server-a", info.getServerName());
    assertEquals("$SYS", info.getSystemTopicPrefix());
    assertEquals("4.5.0", info.getVersion());
    assertEquals("fallback", info.getName());
    assertEquals("_maps._tcp.local.", info.getType());
    assertArrayEquals(new String[]{"10.0.0.1"}, info.getHostAddresses());
    assertEquals("local.", info.getDomain());
    assertEquals(1883, info.getPort());
    assertEquals("maps", info.getApplication());
  }

  @Test
  void serverNameFallsBackToServiceNameAndStripsSuffix() {
    ServiceInfo service = mock(ServiceInfo.class);
    when(service.getPropertyString("server name")).thenReturn(null);
    when(service.getName()).thenReturn("edge-node (2)");

    MapsServiceInfo info = new MapsServiceInfo(service);

    assertEquals("edge-node", info.getServerName());
    assertEquals("", info.getPropertyString("missing"));
    assertFalse(info.supportsSchema());
  }

  @Test
  void additionalPropertiesExcludeReservedMapsMetadata() {
    ServiceInfo service = mock(ServiceInfo.class);
    List<String> names = List.of(
        "protocol", "system topics", "schema name", "schema support",
        "server name", "version", "date", "site", "role"
    );
    when(service.getPropertyNames()).thenReturn(Collections.enumeration(names));
    for (String name : names) {
      when(service.getPropertyString(name)).thenReturn("value-" + name);
    }

    MapsServiceInfo info = new MapsServiceInfo(service);
    Map<String, String> properties = info.getProperties();

    assertEquals(Map.of(
        "site", "value-site",
        "role", "value-role"
    ), properties);
  }
}
