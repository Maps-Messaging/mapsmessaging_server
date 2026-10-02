/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MavlinkStateSubscriberBranchCoverageTest {

  @Test
  void supportedMessageFromUnknownSourceIsFilteredAndCompleted() {
    Fixture fixture = fixture();
    MessageEvent event = event(validHeartbeatJson(), null, null);

    fixture.subscriber.handle(event);

    verify(fixture.sourceRegistry).getKnownSource(any());
    verify(fixture.droneRegistry, never()).getDroneInfo(anyString());
    verify(fixture.twinUpdater, never()).updateTwinState(any(), any(), any(), any(), any());
    verify(event.getCompletionTask()).run();
  }

  @Test
  void configuredSourceWithoutDroneIsFilteredAndCompleted() {
    Fixture fixture = fixture();
    MavlinkKnownSourceDTO source = mock(MavlinkKnownSourceDTO.class);
    when(source.getName()).thenReturn("drone-1");
    when(fixture.sourceRegistry.getKnownSource(any())).thenReturn(source);
    MessageEvent event = event(validHeartbeatJson(), null, null);

    fixture.subscriber.handle(event);

    verify(fixture.droneRegistry).getDroneInfo("drone-1");
    verify(fixture.twinUpdater, never()).updateTwinState(any(), any(), any(), any(), any());
    verify(event.getCompletionTask()).run();
  }

  @Test
  void configuredHeartbeatBuildsExpectedUpdateContext() {
    Fixture fixture = fixture();
    MavlinkKnownSourceDTO source = mock(MavlinkKnownSourceDTO.class);
    DroneInfoDTO droneInfo = mock(DroneInfoDTO.class);
    when(source.getName()).thenReturn("drone-1");
    when(fixture.sourceRegistry.getKnownSource(any())).thenReturn(source);
    when(fixture.droneRegistry.getDroneInfo("drone-1")).thenReturn(droneInfo);
    MessageEvent event = event(validHeartbeatJson(), "/mavlink/out", null);

    fixture.subscriber.handle(event);

    ArgumentCaptor<TwinUpdateContext> captor = ArgumentCaptor.forClass(TwinUpdateContext.class);
    verify(fixture.twinUpdater).updateTwinState(any(), any(), captor.capture(), same(source), same(droneInfo));
    TwinUpdateContext context = captor.getValue();
    assertEquals("mavlink", context.getUpdateSource());
    assertEquals("mavlink:3:1", context.getSourceInstanceId());
    assertEquals(9L, context.getSequenceNumber());
    assertEquals("0", context.getReason());
    assertEquals("/mavlink/out", context.getResponseTopic());
    assertNull(context.getUniqueOutboundIdentifier());
    verify(event.getCompletionTask()).run();
  }


  @Test
  void configuredHeartbeatCopiesCorrelationIdentifierIntoUpdateContext() {
    Fixture fixture = fixture();
    MavlinkKnownSourceDTO source = mock(MavlinkKnownSourceDTO.class);
    DroneInfoDTO droneInfo = mock(DroneInfoDTO.class);
    when(source.getName()).thenReturn("drone-1");
    when(fixture.sourceRegistry.getKnownSource(any())).thenReturn(source);
    when(fixture.droneRegistry.getDroneInfo("drone-1")).thenReturn(droneInfo);
    MessageEvent event = event(validHeartbeatJson(), "/mavlink/out", "ID#42#vehicle");

    fixture.subscriber.handle(event);

    ArgumentCaptor<TwinUpdateContext> captor = ArgumentCaptor.forClass(TwinUpdateContext.class);
    verify(fixture.twinUpdater).updateTwinState(any(), any(), captor.capture(), same(source), same(droneInfo));
    assertEquals("ID#42#vehicle", captor.getValue().getUniqueOutboundIdentifier());
    verify(event.getCompletionTask()).run();
  }

  private Fixture fixture() {
    MavlinkSourceRegistry sourceRegistry = mock(MavlinkSourceRegistry.class);
    DroneInfoRegistry droneRegistry = mock(DroneInfoRegistry.class);
    MavlinkTwinUpdater twinUpdater = mock(MavlinkTwinUpdater.class);
    MavlinkStateSubscriber subscriber = new MavlinkStateSubscriber(
        mock(StateLoopProtocol.class), "mavlink/state", sourceRegistry, droneRegistry, twinUpdater);
    return new Fixture(sourceRegistry, droneRegistry, twinUpdater, subscriber);
  }

  private MessageEvent event(String json, String responseTopic, String correlationData) {
    MessageBuilder builder = new MessageBuilder()
        .setOpaqueData(json.getBytes(StandardCharsets.UTF_8))
        .setResponseTopic(responseTopic);
    if (correlationData != null) {
      builder.setCorrelationData(correlationData.getBytes(StandardCharsets.UTF_8));
    }
    MessageEvent event = mock(MessageEvent.class);
    when(event.getDestinationName()).thenReturn("/mavlink/source");
    when(event.getMessage()).thenReturn(builder.build());
    when(event.getCompletionTask()).thenReturn(mock(Runnable.class));
    return event;
  }

  private String validHeartbeatJson() {
    return """
        {
          "mavlink": {
            "version": "V2",
            "messageId": 0,
            "systemId": 3,
            "componentId": 1,
            "sequence": 9,
            "payloadLength": 9,
            "signed": false,
            "payload": {
              "decoded": {
                "type": 2,
                "autopilot": 3,
                "base_mode": 128,
                "custom_mode": 4,
                "system_status": 4,
                "mavlink_version": 3
              }
            }
          }
        }
        """;
  }

  private record Fixture(
      MavlinkSourceRegistry sourceRegistry,
      DroneInfoRegistry droneRegistry,
      MavlinkTwinUpdater twinUpdater,
      MavlinkStateSubscriber subscriber) {
  }
}
