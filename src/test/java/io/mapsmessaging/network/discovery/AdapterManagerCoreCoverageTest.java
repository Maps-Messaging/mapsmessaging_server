/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.discovery;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPointServer;
import org.junit.jupiter.api.Test;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdapterManagerCoreCoverageTest {

  @Test
  void constructorNormalisesDomainAndExposesAdapter() {
    JmDNS dns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("10.0.0.1", "server", dns, false, "local");

    assertEquals("10.0.0.1", manager.getAdapter());
    assertTrue(manager.getEndPointList().isEmpty());
  }

  @Test
  void directServiceRegistrationAndDeregistrationDelegateToMdns() throws Exception {
    JmDNS dns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("10.0.0.1", "server", dns, false, ".local");
    ServiceInfo info = ServiceInfo.create("_demo._tcp.local.", "demo", 1883, "text");

    manager.register(info);
    manager.deregister(info);

    verify(dns).registerService(info);
    verify(dns).unregisterService(info);
  }

  @Test
  void listenerLifecycleDelegatesToMdns() {
    JmDNS dns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("10.0.0.1", "server", dns, false, "local");
    ServiceListener listener = mock(ServiceListener.class);

    manager.registerListener("_demo._tcp.local.", listener);
    manager.removeListener("_demo._tcp.local.", listener);

    verify(dns).addServiceListener("_demo._tcp.local.", listener);
    verify(dns).removeServiceListener("_demo._tcp.local.", listener);
  }

  @Test
  void endpointRegistrationBuildsOneServicePerProtocol() {
    JmDNS dns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("10.0.0.1", "server", dns, false, "local");
    EndPointServer server = mock(EndPointServer.class);
    EndPointURL url = mock(EndPointURL.class);
    when(server.getUrl()).thenReturn(url);
    when(url.getPort()).thenReturn(1883);

    manager.register(server, "tcp", List.of("mqtt", "stomp", "custom"));

    List<ServiceInfo> infos = manager.getEndPointList().get(server);
    assertNotNull(infos);
    assertEquals(3, infos.size());
    assertTrue(infos.get(0).getType().contains("_mqtt._tcp"));
    assertTrue(infos.get(1).getType().contains("_stomp._tcp"));
    assertTrue(infos.get(2).getType().contains("_custom._tcp"));
  }

  @Test
  void endpointDeregisterRemovesStoredServicesAndUnregistersEach() {
    JmDNS dns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("10.0.0.1", "server", dns, false, "local");
    EndPointServer server = mock(EndPointServer.class);
    ServiceInfo first = ServiceInfo.create("_a._tcp.local.", "a", 1, "");
    ServiceInfo second = ServiceInfo.create("_b._tcp.local.", "b", 2, "");
    manager.getEndPointList().put(server, List.of(first, second));

    manager.deregister(server);

    assertFalse(manager.getEndPointList().containsKey(server));
    verify(dns).unregisterService(first);
    verify(dns).unregisterService(second);
  }

  @Test
  void deregisterUnknownEndpointIsNoOp() {
    JmDNS dns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("10.0.0.1", "server", dns, false, "local");

    assertDoesNotThrow(() -> manager.deregister(mock(EndPointServer.class)));
    verify(dns, never()).unregisterService(any());
  }

  @Test
  void deregisterAllClearsStateAndMdnsServices() {
    JmDNS dns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("10.0.0.1", "server", dns, false, "local");
    manager.getEndPointList().put(mock(EndPointServer.class), List.of());

    manager.deregisterAll();

    assertTrue(manager.getEndPointList().isEmpty());
    verify(dns).unregisterAllServices();
  }

  @Test
  void closeClosesMdnsAndClearsEndpointState() throws Exception {
    JmDNS dns = mock(JmDNS.class);
    AdapterManager manager = new AdapterManager("10.0.0.1", "server", dns, false, "local");
    manager.getEndPointList().put(mock(EndPointServer.class), List.of());

    manager.close();

    verify(dns).close();
    assertTrue(manager.getEndPointList().isEmpty());
  }
}
