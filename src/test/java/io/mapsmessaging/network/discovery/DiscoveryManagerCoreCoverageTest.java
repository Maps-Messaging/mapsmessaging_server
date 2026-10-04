/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.discovery;

import io.mapsmessaging.config.DiscoveryManagerConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.monitor.NetworkEvent;
import io.mapsmessaging.network.monitor.NetworkInterfaceState;
import io.mapsmessaging.network.monitor.NetworkStateChange;
import org.junit.jupiter.api.Test;

import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscoveryManagerCoreCoverageTest {

  @Test
  void disabledStatusReportsDisabled() throws Exception {
    Harness h = harness(false);

    assertEquals(Status.DISABLED, h.manager.getStatus().getStatus());
    assertEquals("Discovery Manager", h.manager.getName());
    assertEquals("Manages the mDNS records", h.manager.getDescription());
  }

  @Test
  void enabledWithoutAdaptersReportsWarning() throws Exception {
    Harness h = harness(true);

    assertEquals(Status.WARN, h.manager.getStatus().getStatus());
    assertEquals("No bound networks", h.manager.getStatus().getComment());
  }

  @Test
  void enabledWithBoundAdapterReportsOk() throws Exception {
    Harness h = harness(true);
    h.adapters.add(mock(AdapterManager.class));

    assertEquals(Status.OK, h.manager.getStatus().getStatus());
  }

  @Test
  void listenerRegistrationFansOutAcrossAdapters() throws Exception {
    Harness h = harness(true);
    AdapterManager first = adapter("10.0.0.1");
    AdapterManager second = adapter("10.0.0.2");
    h.adapters.add(first);
    h.adapters.add(second);
    ServiceListener listener = mock(ServiceListener.class);

    h.manager.registerListener("_maps._tcp.local.", listener);
    h.manager.removeListener("_maps._tcp.local.", listener);

    verify(first).registerListener("_maps._tcp.local.", listener);
    verify(second).registerListener("_maps._tcp.local.", listener);
    verify(first).removeListener("_maps._tcp.local.", listener);
    verify(second).removeListener("_maps._tcp.local.", listener);
  }

  @Test
  void directServiceRegistrationHonoursWildcardHost() throws Exception {
    Harness h = harness(true);
    AdapterManager first = adapter("10.0.0.1");
    AdapterManager second = adapter("10.0.0.2");
    h.adapters.add(first);
    h.adapters.add(second);

    List<ServiceInfo> infos = h.manager.register(
        "::", "_demo._tcp.local.", "demo", 1883, "key=value");

    assertEquals(2, infos.size());
    verify(first).register(any(ServiceInfo.class));
    verify(second).register(any(ServiceInfo.class));
  }

  @Test
  void directServiceRegistrationHonoursSpecificAdapter() throws Exception {
    Harness h = harness(true);
    AdapterManager first = adapter("10.0.0.1");
    AdapterManager second = adapter("10.0.0.2");
    h.adapters.add(first);
    h.adapters.add(second);

    List<ServiceInfo> infos = h.manager.register(
        "10.0.0.2", "_demo._tcp.local.", "demo", 1883, "text");

    assertEquals(1, infos.size());
    verify(first, never()).register(any(ServiceInfo.class));
    verify(second).register(any(ServiceInfo.class));
  }

  @Test
  void endpointRegistrationRoutesWildcardTcpServiceToEveryAdapter() throws Exception {
    Harness h = harness(true);
    AdapterManager first = adapter("10.10.0.1");
    AdapterManager second = adapter("10.10.0.2");
    h.adapters.add(first);
    h.adapters.add(second);
    EndPointServer server = endpointServer("tcp://0.0.0.0:1883", true, "mqtt,stomp");

    h.manager.register(server);

    verify(first).register(same(server), eq("tcp"), eq(List.of("mqtt", "stomp")));
    verify(second).register(same(server), eq("tcp"), eq(List.of("mqtt", "stomp")));
  }

  @Test
  void endpointRegistrationUsesUdpTransportForHmacAndSpecificAdapter() throws Exception {
    Harness h = harness(true);
    AdapterManager first = adapter("10.20.0.1");
    AdapterManager second = adapter("10.20.0.2");
    h.adapters.add(first);
    h.adapters.add(second);
    EndPointServer server = endpointServer("hmac://10.20.0.2:1884", true, "mqtt-sn");

    h.manager.register(server);

    verify(first, never()).register(any(EndPointServer.class), anyString(), anyList());
    verify(second).register(same(server), eq("udp"), eq(List.of("mqtt-sn")));
  }

  @Test
  void undiscoverableEndpointIsIgnored() throws Exception {
    Harness h = harness(true);
    AdapterManager adapter = adapter("10.30.0.1");
    h.adapters.add(adapter);
    EndPointServer server = endpointServer("tcp://0.0.0.0:1883", false, "mqtt");

    h.manager.register(server);

    verifyNoInteractions(adapter);
  }

  @Test
  void deregisterServiceAndAllFanOutAcrossAdapters() throws Exception {
    Harness h = harness(true);
    AdapterManager first = adapter("10.0.0.1");
    AdapterManager second = adapter("10.0.0.2");
    h.adapters.add(first);
    h.adapters.add(second);
    ServiceInfo info = ServiceInfo.create("_demo._tcp.local.", "demo", 1883, "text");

    h.manager.deregister(info);
    h.manager.deregisterAll();

    verify(first).deregister(info);
    verify(second).deregister(info);
    verify(first).deregisterAll();
    verify(second).deregisterAll();
  }

  @Test
  void removedInterfaceClosesAndRemovesMatchingAdapter() throws Exception {
    Harness h = harness(true);
    AdapterManager matching = adapter("10.1.2.3");
    AdapterManager retained = adapter("10.1.2.4");
    h.adapters.add(matching);
    h.adapters.add(retained);

    NetworkInterfaceState state = mock(NetworkInterfaceState.class);
    when(state.getIpAddresses()).thenReturn(List.of(InetAddress.getByName("10.1.2.3")));
    NetworkStateChange change = new NetworkStateChange(NetworkEvent.REMOVED, state);

    h.manager.accept(change);

    assertFalse(h.adapters.contains(matching));
    assertTrue(h.adapters.contains(retained));
    verify(matching).close();
    verify(retained, never()).close();
  }

  @Test
  void downInterfaceUsesSameRemovalPath() throws Exception {
    Harness h = harness(true);
    AdapterManager matching = adapter("10.2.3.4");
    h.adapters.add(matching);

    NetworkInterfaceState state = mock(NetworkInterfaceState.class);
    when(state.getIpAddresses()).thenReturn(List.of(InetAddress.getByName("10.2.3.4")));

    h.manager.accept(new NetworkStateChange(NetworkEvent.DOWN, state));

    assertTrue(h.adapters.isEmpty());
    verify(matching).close();
  }

  @Test
  void unrelatedNetworkEventDoesNotChangeBindings() throws Exception {
    Harness h = harness(true);
    AdapterManager adapter = adapter("10.3.4.5");
    h.adapters.add(adapter);
    NetworkInterfaceState state = mock(NetworkInterfaceState.class);
    when(state.getIpAddresses()).thenReturn(List.of(InetAddress.getByName("10.3.4.5")));

    h.manager.accept(new NetworkStateChange(NetworkEvent.UP, state));

    assertEquals(List.of(adapter), h.adapters);
    verifyNoInteractions(adapter);
  }

  @Test
  void andThenRejectsNullConsumer() throws Exception {
    Harness h = harness(true);

    assertThrows(NullPointerException.class, () -> h.manager.andThen(null));
  }

  private static EndPointServer endpointServer(
      String url, boolean discoverable, String protocols) {
    EndPointServer server = mock(EndPointServer.class);
    EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
    EndPointConfigDTO endPointConfig = mock(EndPointConfigDTO.class);
    when(server.getConfig()).thenReturn(config);
    when(server.getUrl()).thenReturn(new EndPointURL(url));
    when(config.getEndPointConfig()).thenReturn(endPointConfig);
    when(config.getProtocols()).thenReturn(protocols);
    when(endPointConfig.isDiscoverable()).thenReturn(discoverable);
    return server;
  }

  private static AdapterManager adapter(String address) {
    AdapterManager adapter = mock(AdapterManager.class);
    when(adapter.getAdapter()).thenReturn(address);
    return adapter;
  }

  private static Harness harness(boolean enabled) throws Exception {
    DiscoveryManager manager = mock(DiscoveryManager.class, CALLS_REAL_METHODS);
    List<AdapterManager> adapters = new ArrayList<>();
    DiscoveryManagerConfig config = mock(DiscoveryManagerConfig.class);

    set(manager, "logger", mock(Logger.class));
    set(manager, "serverName", "server");
    set(manager, "boundedNetworks", adapters);
    set(manager, "properties", config);
    set(manager, "stampMeta", true);
    set(manager, "domainName", "local");
    set(manager, "enabled", enabled);

    return new Harness(manager, adapters);
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = DiscoveryManager.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private record Harness(DiscoveryManager manager, List<AdapterManager> adapters) {
  }
}
