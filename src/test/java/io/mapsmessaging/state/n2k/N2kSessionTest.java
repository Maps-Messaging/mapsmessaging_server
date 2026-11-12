package io.mapsmessaging.state.n2k;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.util.SessionHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class N2kSessionTest {

  @Test
  void missingDroneConfigurationSkipsLifecycleWork() {
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    N2KTwinConfig config = config();
    DroneInfoRegistry registry = mock(DroneInfoRegistry.class);
    when(registry.getDroneInfo("boat")).thenReturn(null);

    try (MockedStatic<SessionHelper> mocked = mockStatic(SessionHelper.class)) {
      mocked.when(() -> SessionHelper.createLoopbackProtocol(any()))
          .thenReturn(protocol);

      N2kSession session =
          new N2kSession(mock(TwinManager.class), config, registry);

      session.start();
      session.stop();

      verifyNoInteractions(protocol);
    }
  }

  @Test
  void configuredDroneConnectsSubscribesAndClosesLoopbackProtocol() throws Exception {
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    N2KTwinConfig config = config();
    DroneInfoRegistry registry = mock(DroneInfoRegistry.class);
    when(registry.getDroneInfo("boat")).thenReturn(mock(DroneInfoDTO.class));

    try (MockedStatic<SessionHelper> mocked = mockStatic(SessionHelper.class)) {
      mocked.when(() -> SessionHelper.createLoopbackProtocol(any()))
          .thenReturn(protocol);

      N2kSession session =
          new N2kSession(mock(TwinManager.class), config, registry);

      session.start();
      session.stop();

      verify(protocol).connect(anyString(), eq("anonymous"), eq("anonymous"));
      verify(protocol).subscribeLocal(
          eq("/n2k"), eq("/n2k"), eq(QualityOfService.AT_MOST_ONCE),
          isNull(), isNull(), isNull(), isNull(), isNull());
      verify(protocol).unsubscribeLocal("/n2k");
      verify(protocol).close();
    }
  }

  @Test
  void emptyMessageIsIgnoredButCompletionAlwaysRuns() {
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    DroneInfoRegistry registry = mock(DroneInfoRegistry.class);
    when(registry.getDroneInfo("boat")).thenReturn(mock(DroneInfoDTO.class));
    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);
    when(event.getDestinationName()).thenReturn("/n2k");
    when(event.getMessage()).thenReturn(message);
    when(event.getCompletionTask()).thenReturn(completion);
    when(message.getOpaqueData()).thenReturn(new byte[0]);

    try (MockedStatic<SessionHelper> mocked = mockStatic(SessionHelper.class)) {
      mocked.when(() -> SessionHelper.createLoopbackProtocol(any()))
          .thenReturn(protocol);

      N2kSession session =
          new N2kSession(mock(TwinManager.class), config(), registry);
      assertDoesNotThrow(() -> session.handle(event));
    }

    verify(completion).run();
  }

  @Test
  void outboundOnlyN2kSessionPublishesCompletePgnUsingExistingSession() throws Exception {
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    Session engineSession = mock(Session.class);
    Destination destination = mock(Destination.class);
    N2KTwinConfig config = config();
    when(config.isPublishMavlinkDrones()).thenReturn(true);
    when(config.getOutboundTopicName()).thenReturn("/n2k/ais/outbound");
    when(protocol.getSession()).thenReturn(engineSession);
    when(engineSession.findDestination("/n2k/ais/outbound", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));

    try (MockedStatic<SessionHelper> mocked = mockStatic(SessionHelper.class)) {
      mocked.when(() -> SessionHelper.createLoopbackProtocol(any())).thenReturn(protocol);
      N2kSession session = new N2kSession(mock(TwinManager.class), config, mock(DroneInfoRegistry.class), false);

      session.start();
      assertTrue(session.publishPgn(129039, 255, new byte[]{1, 2, 3}));
      session.stop();

      verify(protocol).connect(anyString(), eq("anonymous"), eq("anonymous"));
      verify(protocol, never()).subscribeLocal(anyString(), anyString(), any(), any(), any(), any(), any(), any());
      verify(protocol).close();
      org.mockito.ArgumentCaptor<Message> messages = org.mockito.ArgumentCaptor.forClass(Message.class);
      verify(destination).storeMessage(messages.capture());
      JsonObject request = JsonParser.parseString(new String(messages.getValue().getOpaqueData(), StandardCharsets.UTF_8)).getAsJsonObject();
      assertEquals("n2k-pgn", request.get("type").getAsString());
      assertEquals(129039, request.get("pgn").getAsInt());
      assertEquals(255, request.get("destinationAddress").getAsInt());
      assertArrayEquals(new byte[]{1, 2, 3}, Base64.getDecoder().decode(request.get("payload").getAsString()));
    }
  }

  @Test
  void outboundPublishWithoutStartedSessionIsRejected() {
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    N2KTwinConfig config = config();
    when(config.getOutboundTopicName()).thenReturn("/n2k/ais/outbound");
    try (MockedStatic<SessionHelper> mocked = mockStatic(SessionHelper.class)) {
      mocked.when(() -> SessionHelper.createLoopbackProtocol(any())).thenReturn(protocol);
      N2kSession session = new N2kSession(mock(TwinManager.class), config, mock(DroneInfoRegistry.class), false);

      assertFalse(session.publishPgn(129039, 255, new byte[]{1}));
      verifyNoInteractions(protocol);
    }
  }

  private static N2KTwinConfig config() {
    N2KTwinConfig config = mock(N2KTwinConfig.class);
    when(config.getName()).thenReturn("boat");
    when(config.getTopic()).thenReturn("/n2k");
    return config;
  }
}
