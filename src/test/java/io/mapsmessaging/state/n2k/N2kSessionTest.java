package io.mapsmessaging.state.n2k;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class N2kSessionTest {

  private static final Instant NOW = Instant.parse("2026-07-29T00:30:00Z");

  @Test
  void repeatedStartAndStopAreIdempotent() throws Exception {
    Fixture fixture = new Fixture();

    fixture.session.start();
    fixture.session.start();
    fixture.session.stop();
    fixture.session.stop();

    verify(fixture.protocol).connect(anyString(), eq("anonymous"), eq("anonymous"));
    verify(fixture.protocol).subscribeLocal(
        fixture.config.getTopic(),
        fixture.config.getTopic(),
        QualityOfService.AT_MOST_ONCE,
        null,
        null,
        null,
        null,
        null);
    verify(fixture.protocol).unsubscribeLocal(fixture.config.getTopic());
    verify(fixture.protocol).close();
  }

  @Test
  void stopClosesProtocolWhenUnsubscribeFails() throws Exception {
    Fixture fixture = new Fixture();
    doThrow(new IOException("unsubscribe failed"))
        .when(fixture.protocol)
        .unsubscribeLocal(fixture.config.getTopic());

    fixture.session.start();
    fixture.session.stop();

    verify(fixture.protocol).close();
  }

  @Test
  void failedSubscriptionClosesProtocolAndRejectsEvents() throws Exception {
    Fixture fixture = new Fixture();
    doThrow(new IOException("subscribe failed"))
        .when(fixture.protocol)
        .subscribeLocal(anyString(), anyString(), any(), isNull(), isNull(), isNull(), isNull(), isNull());

    fixture.session.start();
    fixture.session.handle(fixture.event(validEnvelope()));

    verify(fixture.protocol).close();
    verify(fixture.twinUpdater, never()).updateTwinState(anyInt(), any(), any(), any(), any());
    assertEquals(1, fixture.completionCount.get());
  }

  @Test
  void eventAfterStopIsCompletedWithoutUpdatingTwin() {
    Fixture fixture = new Fixture();
    fixture.session.start();
    fixture.session.stop();

    fixture.session.handle(fixture.event(validEnvelope()));

    verify(fixture.twinUpdater, never()).updateTwinState(anyInt(), any(), any(), any(), any());
    assertEquals(1, fixture.completionCount.get());
  }

  @Test
  void validEventUsesDeterministicContextAndCompletes() {
    Fixture fixture = new Fixture();
    fixture.session.start();

    fixture.session.handle(fixture.event(validEnvelope()));

    ArgumentCaptor<TwinUpdateContext> contextCaptor = ArgumentCaptor.forClass(TwinUpdateContext.class);
    verify(fixture.twinUpdater).updateTwinState(
        eq(129025), any(), contextCaptor.capture(), eq(fixture.config), eq(fixture.droneInfo));

    TwinUpdateContext context = contextCaptor.getValue();
    assertEquals("n2k-updater", context.getUpdateSource());
    assertEquals("/canbus0/n2k/json/position:source-17", context.getSourceInstanceId());
    assertEquals(NOW, context.getReceivedTime());
    assertEquals(7L, context.getSequenceNumber());
    assertEquals("Position Rapid Update", context.getReason());
    assertFalse(context.isFullSnapshot());
    assertEquals(1, fixture.completionCount.get());
  }

  @Test
  void malformedJsonCompletesBeforePropagatingFailure() {
    Fixture fixture = new Fixture();
    fixture.session.start();
    MessageEvent event = fixture.event("{");

    assertThrows(RuntimeException.class, () -> fixture.session.handle(event));

    assertEquals(1, fixture.completionCount.get());
  }

  private static String validEnvelope() {
    return """
        {
          "j1939": {
            "pgn": 129025,
            "source": 17,
            "n2k": {
              "name": "Position Rapid Update",
              "packet": {
                "latitude": -33.8688,
                "longitude": 151.2093,
                "sid": 7
              }
            }
          }
        }
        """;
  }

  private static final class Fixture {
    private final StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    private final N2kTwinUpdater twinUpdater = mock(N2kTwinUpdater.class);
    private final N2KTwinConfig config = new N2KTwinConfig();
    private final DroneInfoDTO droneInfo = new DroneInfoDTO();
    private final AtomicInteger completionCount = new AtomicInteger();
    private final N2kSession session;

    private Fixture() {
      config.setName("canbus0-n2k");
      config.setTopic("/canbus0/n2k/json/#");
      session = new N2kSession(
          protocol,
          config,
          twinUpdater,
          droneInfo,
          Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private MessageEvent event(String json) {
      Message message = mock(Message.class);
      when(message.getOpaqueData()).thenReturn(json.getBytes(StandardCharsets.UTF_8));
      return new MessageEvent(
          "/canbus0/n2k/json/position",
          null,
          message,
          completionCount::incrementAndGet);
    }
  }
}
