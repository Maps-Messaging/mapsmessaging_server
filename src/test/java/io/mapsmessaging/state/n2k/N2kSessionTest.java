package io.mapsmessaging.state.n2k;

import io.mapsmessaging.api.MessageEvent;
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

  private static N2KTwinConfig config() {
    N2KTwinConfig config = mock(N2KTwinConfig.class);
    when(config.getName()).thenReturn("boat");
    when(config.getTopic()).thenReturn("/n2k");
    return config;
  }
}
