/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.monitor;

import io.mapsmessaging.config.NetworkManagerConfig;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NetworkInterfaceMonitorCoreCoverageTest {

  @BeforeAll
  static void initializeMonitorWithIsolatedConfiguration() throws Exception {
    try (MockedStatic<NetworkManagerConfig> configuration = mockStatic(NetworkManagerConfig.class)) {
      NetworkManagerConfig config = mock(NetworkManagerConfig.class);
      configuration.when(NetworkManagerConfig::getInstance).thenReturn(config);
      Class.forName(NetworkInterfaceMonitor.class.getName());
    }
  }

  @ParameterizedTest
  @CsvSource({
      "127.0.0.1,127.0.0.1,true",
      "0.0.0.0,127.0.0.1,true",
      "192.0.2.1,127.0.0.1,false",
      "::,::1,true",
      "0.0.0.0,::1,false",
      "::1,::1,true"
  })
  void addressMatchingHandlesExactAndWildcardForms(
      String source, String address, boolean expected) throws Exception {
    NetworkInterfaceMonitor monitor = monitor(false);

    assertEquals(expected, monitor.ipAddressMatches(source, InetAddress.getByName(address)));
  }

  @Test
  void addressLookupFallsBackToLiteralResolutionAndRejectsUnknownHost() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(false);

    assertEquals(
        InetAddress.getByName("127.0.0.1"),
        monitor.getIpAddressByName("127.0.0.1").get(0));
    assertTrue(monitor.getIpAddressByName("mapsmessaging.invalid").isEmpty());
  }

  @Test
  void stopCancelsScheduledScan() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(true);
    ScheduledFuture<?> future = mock(ScheduledFuture.class);
    set(monitor, "scheduledFuture", future);

    monitor.stop();

    verify(future).cancel(true);
  }

  @Test
  void stopWithoutScheduleIsNoOp() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(true);

    assertDoesNotThrow(monitor::stop);
  }

  @Test
  void listenerLifecycleControlsNotifications() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(false);
    Consumer<NetworkStateChange> listener = mock(Consumer.class);
    monitor.addListener(listener);

    NetworkInterfaceState added = state("eth-new", true, "192.0.2.1");
    compare(monitor, Map.of(), Map.of("eth-new", added));
    verify(listener).accept(argThat(change -> change.getEvent() == NetworkEvent.ADDED));

    reset(listener);
    monitor.removeListener(listener);
    compare(monitor, Map.of(), Map.of("eth-new", added));
    verifyNoInteractions(listener);
  }

  @Test
  void compareMapsReportsAddedAndRemovedInterfaces() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(false);
    List<NetworkStateChange> changes = new ArrayList<>();
    monitor.addListener(changes::add);
    NetworkInterfaceState removed = state("old", true, "192.0.2.2");
    NetworkInterfaceState added = state("new", true, "192.0.2.3");

    compare(
        monitor,
        Map.of("old", removed),
        Map.of("new", added));

    assertEquals(2, changes.size());
    assertTrue(changes.stream().anyMatch(c -> c.getEvent() == NetworkEvent.ADDED));
    assertTrue(changes.stream().anyMatch(c -> c.getEvent() == NetworkEvent.REMOVED));
  }

  @Test
  void interfaceUpTransitionProducesUpEvent() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(false);
    List<NetworkStateChange> changes = new ArrayList<>();
    monitor.addListener(changes::add);

    compare(
        monitor,
        Map.of("eth0", state("eth0", false, "192.0.2.4")),
        Map.of("eth0", state("eth0", true, "192.0.2.4")));

    assertTrue(changes.stream().anyMatch(c -> c.getEvent() == NetworkEvent.UP));
  }

  @Test
  void interfaceDownTransitionProducesDownEvent() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(false);
    List<NetworkStateChange> changes = new ArrayList<>();
    monitor.addListener(changes::add);

    compare(
        monitor,
        Map.of("eth0", state("eth0", true, "192.0.2.5")),
        Map.of("eth0", state("eth0", false, "192.0.2.5")));

    assertEquals(1, changes.size());
    assertEquals(NetworkEvent.DOWN, changes.get(0).getEvent());
  }

  @Test
  void addressCountChangeProducesDownThenIpChanged() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(false);
    List<NetworkStateChange> changes = new ArrayList<>();
    monitor.addListener(changes::add);
    NetworkInterfaceState oldState = state("eth0", true, "192.0.2.6");
    NetworkInterfaceState newState = state("eth0", true, "192.0.2.6", "192.0.2.7");

    compare(monitor, Map.of("eth0", oldState), Map.of("eth0", newState));

    assertEquals(List.of(NetworkEvent.DOWN, NetworkEvent.IP_CHANGED),
        changes.stream().map(NetworkStateChange::getEvent).toList());
  }

  @Test
  void changedAddressProducesDownThenIpChanged() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(false);
    List<NetworkStateChange> changes = new ArrayList<>();
    monitor.addListener(changes::add);

    compare(
        monitor,
        Map.of("eth0", state("eth0", true, "192.0.2.8")),
        Map.of("eth0", state("eth0", true, "192.0.2.9")));

    assertEquals(List.of(NetworkEvent.DOWN, NetworkEvent.IP_CHANGED),
        changes.stream().map(NetworkStateChange::getEvent).toList());
  }

  @Test
  void unchangedInterfaceProducesNoEvent() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(false);
    List<NetworkStateChange> changes = new ArrayList<>();
    monitor.addListener(changes::add);

    compare(
        monitor,
        Map.of("eth0", state("eth0", true, "192.0.2.10")),
        Map.of("eth0", state("eth0", true, "192.0.2.10")));

    assertTrue(changes.isEmpty());
  }

  @Test
  void statusReflectsEnabledAndTrackedInterfaceCount() throws Exception {
    NetworkInterfaceMonitor enabled = monitor(true);
    set(enabled, "lastInterfaces", Map.of("a", state("a", true, "192.0.2.11")));
    assertEquals(Status.OK, enabled.getStatus().getStatus());
    assertTrue(enabled.getStatus().getComment().contains("1"));

    NetworkInterfaceMonitor disabled = monitor(false);
    set(disabled, "lastInterfaces", Map.of());
    assertEquals(Status.DISABLED, disabled.getStatus().getStatus());
  }

  @Test
  void identityStringsDescribeSubsystem() throws Exception {
    NetworkInterfaceMonitor monitor = monitor(false);

    assertEquals("Network Interface Monitor", monitor.getName());
    assertEquals(
        "Monitors system network interfaces and updates end points dependent on the network state",
        monitor.getDescription());
  }

  private static NetworkInterfaceState state(String name, boolean up, String... addresses)
      throws Exception {
    NetworkInterfaceState state = mock(NetworkInterfaceState.class);
    when(state.getName()).thenReturn(name);
    when(state.isUp()).thenReturn(up);
    List<InetAddress> values = new ArrayList<>();
    for (String address : addresses) {
      values.add(InetAddress.getByName(address));
    }
    when(state.getIpAddresses()).thenReturn(values);
    return state;
  }

  private static void compare(
      NetworkInterfaceMonitor monitor,
      Map<String, NetworkInterfaceState> oldMap,
      Map<String, NetworkInterfaceState> newMap) throws Exception {
    Method method = NetworkInterfaceMonitor.class.getDeclaredMethod(
        "compareInterfaceMaps", Map.class, Map.class);
    method.setAccessible(true);
    method.invoke(monitor, oldMap, newMap);
  }

  private static NetworkInterfaceMonitor monitor(boolean enabled) throws Exception {
    NetworkInterfaceMonitor monitor = mock(NetworkInterfaceMonitor.class, CALLS_REAL_METHODS);
    set(monitor, "logger", mock(Logger.class));
    set(monitor, "listeners", new ArrayList<Consumer<NetworkStateChange>>());
    set(monitor, "enabled", enabled);
    set(monitor, "interval", 1L);
    set(monitor, "scheduledFuture", null);
    set(monitor, "lastInterfaces", new LinkedHashMap<String, NetworkInterfaceState>());
    return monitor;
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = NetworkInterfaceMonitor.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
