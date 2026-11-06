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

package io.mapsmessaging.network.io.impl.udp.session;

import io.mapsmessaging.network.io.Timeoutable;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.UnixDomainSocketAddress;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class UDPSessionManagerAddressTest {

  private UDPSessionState<Timeoutable> state() {
    UDPSessionState<Timeoutable> state = new UDPSessionState<>(mock(Timeoutable.class));
    state.setClientIdentifier("client");
    return state;
  }

  @Test
  void same_host_can_change_port_without_enabling_host_changes() {
    UDPSessionManager<Timeoutable> manager = new UDPSessionManager<>(60);
    try {
      var original = InetSocketAddress.createUnresolved("sensor.example", 1000);
      var updated = InetSocketAddress.createUnresolved("sensor.example", 2000);
      var state = state();
      manager.addState(original, state);
      assertSame(state, manager.findAndUpdate("client", updated, false));
      assertNull(manager.getState(original));
      assertSame(state, manager.getState(updated));
    } finally {
      manager.close();
    }
  }

  @Test
  void different_host_is_rejected_until_address_changes_are_enabled() {
    UDPSessionManager<Timeoutable> manager = new UDPSessionManager<>(60);
    try {
      var original = InetSocketAddress.createUnresolved("sensor.example", 1000);
      var updated = InetSocketAddress.createUnresolved("replacement.example", 1000);
      var state = state();
      manager.addState(original, state);
      assertNull(manager.findAndUpdate("client", updated, false));
      assertSame(state, manager.getState(original));
      assertNull(manager.getState(updated));
      assertSame(state, manager.findAndUpdate("client", updated, true));
      assertNull(manager.getState(original));
      assertSame(state, manager.getState(updated));
    } finally {
      manager.close();
    }
  }

  @Test
  void non_internet_updated_address_is_rejected_even_when_changes_are_enabled() {
    UDPSessionManager<Timeoutable> manager = new UDPSessionManager<>(60);
    try {
      var original = InetSocketAddress.createUnresolved("sensor.example", 1000);
      var state = state();
      manager.addState(original, state);
      assertNull(manager.findAndUpdate("client", UnixDomainSocketAddress.of("msg356-test"), true));
      assertSame(state, manager.getState(original));
    } finally {
      manager.close();
    }
  }

  @Test
  void non_internet_original_address_is_rejected_and_unknown_clients_do_not_migrate() {
    UDPSessionManager<Timeoutable> manager = new UDPSessionManager<>(60);
    try {
      var original = UnixDomainSocketAddress.of("msg356-test");
      var updated = InetSocketAddress.createUnresolved("sensor.example", 1000);
      var state = state();
      manager.addState(original, state);
      assertNull(manager.findAndUpdate("client", updated, true));
      assertNull(manager.findAndUpdate("unknown", updated, true));
      assertSame(state, manager.getState(original));
      assertNull(manager.getState(updated));
    } finally {
      manager.close();
    }
  }
}
