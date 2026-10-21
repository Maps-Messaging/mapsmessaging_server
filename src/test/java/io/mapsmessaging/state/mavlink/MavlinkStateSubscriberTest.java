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

package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MavlinkStateSubscriberTest {

  @Test
  void integrationSourceUsesConfiguredValuesAndSafeFallbacks() {
    MavlinkTwinConfigDTO configured = mock(MavlinkTwinConfigDTO.class);
    when(configured.getName()).thenReturn("primary");
    when(configured.getTopic()).thenReturn("/mavlink/+");

    assertEquals("primary|/mavlink/+", MavlinkStateSubscriber.integrationSource(configured));

    MavlinkTwinConfigDTO defaults = mock(MavlinkTwinConfigDTO.class);
    when(defaults.getName()).thenReturn(" ");
    when(defaults.getTopic()).thenReturn(null);

    assertEquals("mavlink|unknown", MavlinkStateSubscriber.integrationSource(defaults));
  }

  @Test
  void malformedAndIncompleteMessagesAreIgnoredAndCompleted() {
    Fixture fixture = fixture();

    assertMalformedCompleted(fixture, null);
    assertMalformedCompleted(fixture, new byte[0]);
    assertMalformedCompleted(fixture, "not-json".getBytes(StandardCharsets.UTF_8));
    assertMalformedCompleted(fixture, "{}".getBytes(StandardCharsets.UTF_8));
    assertMalformedCompleted(
        fixture,
        "{\"mavlink\":{\"messageId\":42}}".getBytes(StandardCharsets.UTF_8)
    );

    verify(fixture.sourceRegistry, never()).getKnownSource(org.mockito.ArgumentMatchers.any());
    verify(fixture.twinUpdater, never()).updateTwinState(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()
    );
  }

  @Test
  void repeated_start_and_stop_are_idempotent() throws Exception {
    Fixture fixture = fixture();

    fixture.subscriber.start();
    fixture.subscriber.start();

    assertEquals(1, invocationCount(fixture.protocol, "connect"));
    assertEquals(1, invocationCount(fixture.protocol, "subscribeLocal"));

    fixture.subscriber.stop();
    fixture.subscriber.stop();

    assertEquals(1, invocationCount(fixture.protocol, "unsubscribeLocal"));
    assertEquals(1, invocationCount(fixture.protocol, "close"));
    verify(fixture.twinUpdater, times(1)).close();
  }

  @Test
  void stop_closes_protocol_and_monitor_when_unsubscribe_fails() throws Exception {
    Fixture fixture = fixture();
    fixture.subscriber.start();
    RuntimeException failure = new IllegalStateException("unsubscribe failed");
    doThrow(failure).when(fixture.protocol).unsubscribeLocal("mavlink/state");

    RuntimeException thrown = assertThrows(RuntimeException.class, fixture.subscriber::stop);

    assertEquals(failure, thrown);
    assertEquals(1, invocationCount(fixture.protocol, "close"));
    verify(fixture.twinUpdater).close();
  }

  @Test
  void startFailureClosesProtocolAndTwinUpdater() throws Exception {
    Fixture fixture = fixture();
    IOException failure = new IOException("connect failed");
    doThrow(failure).when(fixture.protocol).connect(
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString()
    );

    assertEquals(failure, assertThrows(IOException.class, fixture.subscriber::start));
    assertEquals(1, invocationCount(fixture.protocol, "close"));
    verify(fixture.twinUpdater).close();
    assertThrows(IllegalStateException.class, fixture.subscriber::start);
  }


  @Test
  void startFailureRetainsProtocolAndUpdaterCleanupFailuresAsSuppressed() throws Exception {
    Fixture fixture = fixture();
    IOException startFailure = new IOException("connect failed");
    IOException protocolCloseFailure = new IOException("protocol close failed");
    RuntimeException updaterCloseFailure = new IllegalStateException("updater close failed");
    doThrow(startFailure).when(fixture.protocol).connect(
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString()
    );
    doThrow(protocolCloseFailure).when(fixture.protocol).close();
    doThrow(updaterCloseFailure).when(fixture.twinUpdater).close();

    IOException thrown = assertThrows(IOException.class, fixture.subscriber::start);

    assertSame(startFailure, thrown);
    assertEquals(2, thrown.getSuppressed().length);
    assertSame(protocolCloseFailure, thrown.getSuppressed()[0]);
    assertSame(updaterCloseFailure, thrown.getSuppressed()[1]);
  }

  @Test
  void stopPropagatesProtocolCloseFailureAndSuppressesUpdaterCloseFailure() throws Exception {
    Fixture fixture = fixture();
    IOException protocolCloseFailure = new IOException("protocol close failed");
    RuntimeException updaterCloseFailure = new IllegalStateException("updater close failed");
    doThrow(protocolCloseFailure).when(fixture.protocol).close();
    doThrow(updaterCloseFailure).when(fixture.twinUpdater).close();

    IOException thrown = assertThrows(IOException.class, fixture.subscriber::stop);

    assertSame(protocolCloseFailure, thrown);
    assertEquals(1, thrown.getSuppressed().length);
    assertSame(updaterCloseFailure, thrown.getSuppressed()[0]);
  }

  @Test
  void start_after_stop_is_rejected() throws Exception {
    Fixture fixture = fixture();
    fixture.subscriber.stop();

    assertThrows(IllegalStateException.class, fixture.subscriber::start);
    assertEquals(0, invocationCount(fixture.protocol, "connect"));
    assertEquals(1, invocationCount(fixture.protocol, "close"));
  }

  @Test
  void late_message_after_stop_only_runs_completion() throws Exception {
    Fixture fixture = fixture();
    MessageEvent messageEvent = mock(MessageEvent.class);
    Runnable completionTask = mock(Runnable.class);
    when(messageEvent.getCompletionTask()).thenReturn(completionTask);
    fixture.subscriber.stop();

    fixture.subscriber.handle(messageEvent);

    verify(completionTask).run();
    verify(messageEvent, never()).getDestinationName();
    verify(messageEvent, never()).getMessage();
    verify(fixture.twinUpdater, never()).updateTwinState(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()
    );
  }

  private void assertMalformedCompleted(Fixture fixture, byte[] payload) {
    Message message = new MessageBuilder().setOpaqueData(payload).build();
    MessageEvent messageEvent = mock(MessageEvent.class);
    Runnable completionTask = mock(Runnable.class);
    when(messageEvent.getDestinationName()).thenReturn("/mavlink/source");
    when(messageEvent.getMessage()).thenReturn(message);
    when(messageEvent.getCompletionTask()).thenReturn(completionTask);

    assertDoesNotThrow(() -> fixture.subscriber.handle(messageEvent));

    verify(completionTask).run();
  }

  private long invocationCount(Object mock, String methodName) {
    return mockingDetails(mock).getInvocations().stream()
        .map(Invocation::getMethod)
        .filter(method -> method.getName().equals(methodName))
        .count();
  }

  private Fixture fixture() {
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    MavlinkSourceRegistry sourceRegistry = mock(MavlinkSourceRegistry.class);
    DroneInfoRegistry droneRegistry = mock(DroneInfoRegistry.class);
    MavlinkTwinUpdater twinUpdater = mock(MavlinkTwinUpdater.class);
    MavlinkStateSubscriber subscriber = new MavlinkStateSubscriber(
        protocol,
        "mavlink/state",
        sourceRegistry,
        droneRegistry,
        twinUpdater
    );
    return new Fixture(protocol, sourceRegistry, twinUpdater, subscriber);
  }

  private record Fixture(
      StateLoopProtocol protocol,
      MavlinkSourceRegistry sourceRegistry,
      MavlinkTwinUpdater twinUpdater,
      MavlinkStateSubscriber subscriber
  ) {
  }
}
