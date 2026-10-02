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

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TakTwinObserverCyberIconTest {

  private static final String CYBER_UID = "uid=\"asset-mti-cyber\"";
  private static final String ASSET_UID = "uid=\"asset\"";

  @Test
  void hold_publishes_asset_then_cyber_marker_and_clearing_takes_the_marker_off_once() throws Exception {
    withObserver(socket -> observer -> {
      DroneTwin twin = uavTwin();

      MtiStatusRegistry.setDelegate(
          twinId -> new MtiLookupResult(null, -65536, "MTI: hold", false, "exploit_64x64.png"));
      observer.onTwinAdded(twin, context());
      List<String> held = sent(socket, 2);
      assertTrue(held.get(0).contains(ASSET_UID));
      assertFalse(held.get(0).contains("usericon"));
      assertTrue(held.get(1).contains(CYBER_UID));
      assertTrue(held.get(1).contains("cyber_icons/exploit_64x64.png"));

      MtiStatusRegistry.setDelegate(null);
      clearInvocations(socket);
      statusChanged(observer, twin);
      List<String> cleared = sent(socket, 2);
      assertTrue(cleared.get(0).contains(ASSET_UID));
      assertTrue(cleared.get(1).contains(CYBER_UID));
      assertFalse(cleared.get(1).contains("usericon"));

      clearInvocations(socket);
      statusChanged(observer, twin);
      List<String> afterwards = sent(socket, 1);
      assertTrue(afterwards.get(0).contains(ASSET_UID));
    });
  }

  @Test
  void removing_a_twin_with_a_cyber_marker_also_takes_the_marker_off() throws Exception {
    withObserver(socket -> observer -> {
      DroneTwin twin = uavTwin();
      MtiStatusRegistry.setDelegate(
          twinId -> new MtiLookupResult(null, -65536, "MTI: hold", false, "exploit_64x64.png"));
      observer.onTwinAdded(twin, context());

      clearInvocations(socket);
      observer.onTwinRemoved(twin, context());

      List<String> removed = sent(socket, 2);
      assertTrue(removed.get(0).contains(ASSET_UID));
      assertTrue(removed.get(1).contains(CYBER_UID));
      assertFalse(removed.get(1).contains("usericon"));
    });
  }

  @Test
  void removing_a_twin_without_a_cyber_marker_sends_only_the_asset_removal() throws Exception {
    withObserver(socket -> observer -> {
      DroneTwin twin = uavTwin();
      observer.onTwinAdded(twin, context());

      clearInvocations(socket);
      observer.onTwinRemoved(twin, context());

      assertTrue(sent(socket, 1).get(0).contains(ASSET_UID));
    });
  }

  private List<String> sent(TakSocketConnection socket, int expected) {
    ArgumentCaptor<String> xml = ArgumentCaptor.forClass(String.class);
    verify(socket, times(expected)).accept(xml.capture());
    return xml.getAllValues();
  }

  private void statusChanged(TakTwinObserver observer, DroneTwin twin) {
    observer.onTwinStatusChanged(
        twin.getTwinId(), TwinLifecycleStatus.ACTIVE, TwinLifecycleStatus.ACTIVE, twin, context());
  }

  private void withObserver(ObserverAction action) throws Exception {
    boolean enabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    ConfigurationManager config = mock(ConfigurationManager.class);
    try (MockedStatic<ConfigurationManager> mocked = mockStatic(ConfigurationManager.class)) {
      mocked.when(ConfigurationManager::getInstance).thenReturn(config);
      TakTwinObserver observer = new TakTwinObserver(new TwinManager());
      try {
        TakSocketConnection socket = mock(TakSocketConnection.class);
        setField(observer, "takHost", "test-host");
        setField(observer, "takPort", 1234);
        TakTwinContext twinContext = new TakTwinContext();
        twinContext.setSocketConnection(socket);
        @SuppressWarnings("unchecked")
        Map<String, TakTwinContext> contexts = (Map<String, TakTwinContext>) field(observer, "takContexts");
        contexts.put("asset", twinContext);
        action.with(socket).run(observer);
      } finally {
        observer.shutdown();
      }
    } finally {
      MtiStatusRegistry.setDelegate(null);
      JMXManager.setEnableJMX(enabled);
    }
  }

  private DroneTwin uavTwin() {
    DroneTwin twin = new DroneTwin("asset");
    twin.setVehicleClass(VehicleClass.UAV);
    twin.setGeoPosition(new GeoPosition(38.4, -9.1, 125.0, null, null));
    twin.setLastSeenAt(Instant.now());
    return twin;
  }

  private TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.now());
    return context;
  }

  private Object field(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private interface ObserverAction {
    ObserverStep with(TakSocketConnection socket);
  }

  private interface ObserverStep {
    void run(TakTwinObserver observer) throws Exception;
  }
}
