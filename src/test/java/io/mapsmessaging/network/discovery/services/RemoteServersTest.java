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

class RemoteServersTest {

  @Test
  void constructorCopiesServerMetadataAndInitialService() {
    MapsServiceInfo info = service("server-a", "mqtt", "mqtt", 1883, new String[]{"10.0.0.1"});

    RemoteServers remote = new RemoteServers(info);

    assertEquals("server-a", remote.getServerName());
    assertEquals("/schema", remote.getSchemaPrefix());
    assertTrue(remote.isSchemaSupport());
    assertEquals("$SYS", remote.getSystemTopicPrefix());
    assertEquals("2026-09-19", remote.getBuildDate());
    assertEquals("4.5.0", remote.getVersion());
    assertEquals(1, remote.getServices().size());
    assertTrue(remote.getServices().containsKey("mqtt"));
  }

  @Test
  void updateMergesAddressesForExistingApplicationAndAddsNewProtocol() {
    RemoteServers remote =
        new RemoteServers(service("server-a", "mqtt", "mqtt", 1883, new String[]{"10.0.0.1"}));

    remote.update(service("server-a", "mqtt", "mqtt", 1883, new String[]{"10.0.0.1", "10.0.0.2"}));
    remote.update(service("server-a", "stomp", "stomp", 61613, new String[]{"10.0.0.3"}));

    assertEquals(2, remote.getServices().size());
    assertEquals(
        java.util.List.of("10.0.0.1", "10.0.0.2"),
        remote.getServices().get("mqtt").getAddresses()
    );
    assertTrue(remote.getServices().containsKey("stomp"));
  }

  @Test
  void removeDropsServiceByApplicationAndStringIncludesServerAndServices() {
    MapsServiceInfo mqtt = service("server-a", "mqtt", "mqtt", 1883, new String[]{"10.0.0.1"});
    RemoteServers remote = new RemoteServers(mqtt);

    String text = remote.toString();
    assertTrue(text.contains("server-a"));
    assertTrue(text.contains("Version:4.5.0"));
    assertTrue(text.contains("mqtt:1883"));

    remote.remove(mqtt);
    assertTrue(remote.getServices().isEmpty());
  }

  private static MapsServiceInfo service(
      String serverName,
      String application,
      String protocol,
      int port,
      String[] addresses
  ) {
    MapsServiceInfo info = mock(MapsServiceInfo.class);
    when(info.getServerName()).thenReturn(serverName);
    when(info.getSchemaPrefix()).thenReturn("/schema");
    when(info.supportsSchema()).thenReturn(true);
    when(info.getSystemTopicPrefix()).thenReturn("$SYS");
    when(info.getBuildDate()).thenReturn("2026-09-19");
    when(info.getVersion()).thenReturn("4.5.0");
    when(info.getApplication()).thenReturn(application);
    when(info.getProtocol()).thenReturn(protocol);
    when(info.getPort()).thenReturn(port);
    when(info.getHostAddresses()).thenReturn(addresses);
    when(info.getProperties()).thenReturn(Map.of("role", "broker"));
    return info;
  }
}
