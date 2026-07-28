/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at:
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

package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.listener.ListenerManager;
import io.mapsmessaging.state.mavlink.packet.BatteryStatusPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.BATTERY_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MavlinkTwinUpdaterTest {

  private static final Instant NOW = Instant.parse("2026-07-28T00:00:00Z");

  @Test
  void valid_battery_packet_refreshes_power_and_preserves_existing_response_topic() {
    TwinManager twinManager = twinManager();
    DroneTwin twin = new DroneTwin("drone-1");
    twin.setResponseTopicName("mavlink/original");
    twinManager.registerTwin(twin, context(null, null));
    ListenerManager listenerManager = mock(ListenerManager.class);
    MavlinkTwinUpdater updater = new MavlinkTwinUpdater(twinManager, listenerManager);
    BatteryStatusPacket packet = mock(BatteryStatusPacket.class);
    when(packet.isValid()).thenReturn(true);
    ProcessedFrame frame = frame(17, 42, BATTERY_STATUS);
    TwinUpdateContext context = context("mavlink/replacement", "outbound-1");

    updater.updateTwinState(frame, packet, context, knownSource(), new DroneInfoDTO());

    assertEquals(17, twin.getSystemId());
    assertEquals(42, twin.getComponentId());
    assertEquals(NOW, twin.getPowerUpdatedAt());
    assertEquals("mavlink/original", twin.getResponseTopicName());
    assertEquals("outbound-1", twin.getUniqueOutboundIdentifier());
    verify(listenerManager).handle(BATTERY_STATUS, "drone-1", packet, context);
    updater.close();
  }

  @Test
  void invalid_battery_packet_does_not_refresh_power_timestamp() {
    TwinManager twinManager = twinManager();
    DroneTwin twin = new DroneTwin("drone-1");
    twinManager.registerTwin(twin, context(null, null));
    ListenerManager listenerManager = mock(ListenerManager.class);
    MavlinkTwinUpdater updater = new MavlinkTwinUpdater(twinManager, listenerManager);
    BatteryStatusPacket packet = mock(BatteryStatusPacket.class);
    when(packet.isValid()).thenReturn(false);

    updater.updateTwinState(
        frame(17, 42, BATTERY_STATUS),
        packet,
        context(null, null),
        knownSource(),
        new DroneInfoDTO()
    );

    assertNull(twin.getPowerUpdatedAt());
    updater.close();
  }

  @Test
  void new_twin_uses_known_source_and_drone_configuration() {
    TwinManager twinManager = twinManager();
    ListenerManager listenerManager = mock(ListenerManager.class);
    MavlinkTwinUpdater updater = new MavlinkTwinUpdater(twinManager, listenerManager);
    MavlinkKnownSourceDTO knownSource = knownSource();
    knownSource.setDescription("Survey aircraft");
    knownSource.setVehicleClass(VehicleClass.UAV);
    DroneInfoDTO droneInfo = new DroneInfoDTO();
    UUID uuid = UUID.fromString("d972348c-8496-45de-b130-9c003d7bf245");
    droneInfo.setUuid(uuid);
    droneInfo.setBatteryCapacityHours(4.5);
    droneInfo.setDescription(Map.of("role", "survey"));
    MavlinkPacket packet = mock(MavlinkPacket.class);
    ProcessedFrame frame = frame(17, 42, 999);
    TwinUpdateContext context = context("mavlink/outbound", "outbound-2");

    updater.updateTwinState(frame, packet, context, knownSource, droneInfo);

    DroneTwin twin = (DroneTwin) twinManager.getTwin("drone-1").orElseThrow();
    assertEquals(uuid, twin.getUuid());
    assertEquals("Survey aircraft", twin.getDisplayName());
    assertEquals("Survey aircraft", twin.getDescriptionString());
    assertEquals("drone-1", twin.getCallSign());
    assertEquals(4.5, twin.getBatteryCapacityHours());
    assertEquals("mavlink/outbound", twin.getResponseTopicName());
    assertEquals("outbound-2", twin.getUniqueOutboundIdentifier());
    assertEquals("survey", twin.getDescription().get("role"));
    updater.close();
  }

  @Test
  void close_is_idempotent_and_late_updates_are_ignored() {
    TwinManager twinManager = mock(TwinManager.class);
    ListenerManager listenerManager = mock(ListenerManager.class);
    MavlinkTwinUpdater updater = new MavlinkTwinUpdater(twinManager, listenerManager);

    updater.close();
    updater.close();

    verify(twinManager, times(1)).removeObserver(org.mockito.ArgumentMatchers.any(MavlinkDroneMonitor.class));
    clearInvocations(twinManager, listenerManager);

    updater.updateTwinState(
        frame(17, 42, 999),
        mock(MavlinkPacket.class),
        context(null, null),
        knownSource(),
        new DroneInfoDTO()
    );

    verifyNoInteractions(twinManager, listenerManager);
  }

  private TwinManager twinManager() {
    return new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
  }

  private MavlinkKnownSourceDTO knownSource() {
    MavlinkKnownSourceDTO knownSource = new MavlinkKnownSourceDTO();
    knownSource.setName("drone-1");
    knownSource.setSystemId(17);
    knownSource.setComponentId(42);
    return knownSource;
  }

  private TwinUpdateContext context(String responseTopic, String outboundIdentifier) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(NOW);
    context.setResponseTopic(responseTopic);
    context.setUniqueOutboundIdentifier(outboundIdentifier);
    return context;
  }

  private ProcessedFrame frame(int systemId, int componentId, int messageId) {
    ProcessedFrame processedFrame = mock(ProcessedFrame.class, RETURNS_DEEP_STUBS);
    when(processedFrame.getFrame().getSystemId()).thenReturn(systemId);
    when(processedFrame.getFrame().getComponentId()).thenReturn(componentId);
    when(processedFrame.getFrame().getMessageId()).thenReturn(messageId);
    return processedFrame;
  }
}
