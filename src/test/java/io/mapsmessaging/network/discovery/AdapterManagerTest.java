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

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdapterManagerTest {

  @Test
  void directServiceRegistrationAndDeregistrationDelegateToMdns() throws IOException {
    JmDNS mdns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("eth0", "server", mdns, false, "local.");
    ServiceInfo info = mock(ServiceInfo.class);

    manager.register(info);
    manager.deregister(info);

    verify(mdns).registerService(info);
    verify(mdns).unregisterService(info);
    assertEquals("eth0", manager.getAdapter());
    assertTrue(manager.getEndPointList().isEmpty());
  }

  @Test
  void listenerRegistrationAndRemovalDelegateToMdns() {
    JmDNS mdns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("eth0", "server", mdns, false, ".local.");
    ServiceListener listener = mock(ServiceListener.class);

    manager.registerListener("_mqtt._tcp.local.", listener);
    manager.removeListener("_mqtt._tcp.local.", listener);

    verify(mdns).addServiceListener("_mqtt._tcp.local.", listener);
    verify(mdns).removeServiceListener("_mqtt._tcp.local.", listener);
  }

  @Test
  void deregisterAllClearsTrackedServicesAndCloseClosesMdns() throws IOException {
    JmDNS mdns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("eth0", "server", mdns, false, ".local.");

    manager.deregisterAll();
    assertTrue(manager.getEndPointList().isEmpty());
    verify(mdns).unregisterAllServices();

    manager.close();
    verify(mdns).close();
    assertTrue(manager.getEndPointList().isEmpty());
  }
}
