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

package io.mapsmessaging.state.mavlink.listener;

import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.packet.NamedValueFloatPacket;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NamedValueFloatListenerTest {

  @Test
  void presentDetectionCreatesAndThenUpdatesSingleContact() {
    TwinManager manager = managerWithDrone();
    NamedValueFloatListener listener = new NamedValueFloatListener(manager);
    TwinUpdateContext context = context("2026-09-19T20:00:00Z");

    listener.handle("drone-1", packet("target-a", 1.0, true), context);
    listener.handle("drone-1", packet("target-a", 1.0, true), context);

    DroneTwin twin = (DroneTwin) manager.getTwin("drone-1").orElseThrow();
    assertEquals(1, twin.getContactList().size());
    assertEquals("target-a", twin.getContactList().get(0).getDescription());
    assertEquals(twin.getGeoPosition(), twin.getContactList().get(0).getPosition());
    assertEquals(NamedValueFloatListener.CONTACT_TTL_MILLIS, twin.getContactList().get(0).getTtlMillis());
    assertEquals(context.getReceivedTime(), twin.getOperationalUpdatedAt());
  }

  @Test
  void lostDetectionRemovesExistingContact() {
    TwinManager manager = managerWithDrone();
    NamedValueFloatListener listener = new NamedValueFloatListener(manager);
    TwinUpdateContext context = context("2026-09-19T20:01:00Z");

    listener.handle("drone-1", packet("target-a", 1.0, true), context);
    assertEquals(1, ((DroneTwin) manager.getTwin("drone-1").orElseThrow()).getContactList().size());

    listener.handle("drone-1", packet("target-a", 0.0, true), context);

    assertTrue(((DroneTwin) manager.getTwin("drone-1").orElseThrow()).getContactList().isEmpty());
  }

  @Test
  void invalidMissingNameOrMissingValuePacketsAreIgnored() {
    TwinManager manager = managerWithDrone();
    NamedValueFloatListener listener = new NamedValueFloatListener(manager);
    TwinUpdateContext context = context("2026-09-19T20:02:00Z");

    listener.handle("drone-1", packet("target-a", 1.0, false), context);
    listener.handle("drone-1", packet("", 1.0, true), context);
    listener.handle("drone-1", packetWithoutValue("target-a"), context);

    DroneTwin twin = (DroneTwin) manager.getTwin("drone-1").orElseThrow();
    assertTrue(twin.getContactList().isEmpty());
    assertNull(twin.getOperationalUpdatedAt());
  }

  private static TwinManager managerWithDrone() {
    TwinManager manager = new TwinManager();
    DroneTwin twin = new DroneTwin("drone-1");
    twin.setGeoPosition(new GeoPosition(38.4, -9.1, 10.0, null, null));
    manager.registerTwin(twin, new TwinUpdateContext());
    return manager;
  }

  private static TwinUpdateContext context(String time) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse(time));
    return context;
  }

  private static NamedValueFloatPacket packet(String name, double value, boolean valid) {
    return new NamedValueFloatPacket(new ProcessedFrame(
        "NAMED_VALUE_FLOAT",
        null,
        Map.of("time_boot_ms", 100L, "name", name, "value", value),
        valid,
        List.of(),
        null
    ));
  }

  private static NamedValueFloatPacket packetWithoutValue(String name) {
    return new NamedValueFloatPacket(new ProcessedFrame(
        "NAMED_VALUE_FLOAT",
        null,
        Map.of("time_boot_ms", 100L, "name", name),
        true,
        List.of(),
        null
    ));
  }
}
