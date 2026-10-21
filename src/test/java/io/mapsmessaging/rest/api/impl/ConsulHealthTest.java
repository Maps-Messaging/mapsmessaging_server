package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsulHealthTest {

  @Test
  void activeEndpointAlwaysReturnsOk() {
    Response response = new ConsulHealth().isActive();

    assertEquals(200, response.getStatus());
    assertEquals("Ok", response.getEntity());
  }

  @Test
  void warningSubsystemReturnsWarningButStillHealthyStatusCode() {
    MessageDaemon daemon = mock(MessageDaemon.class, RETURNS_DEEP_STUBS);
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setStatus(Status.WARN);
    when(daemon.getSubSystemManager().getSubSystemStatus())
        .thenReturn(List.of(status));

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      Response response = new ConsulHealth().getHealth();

      assertEquals(200, response.getStatus());
      assertEquals("Warning", response.getEntity());
    }
  }

  @Test
  void errorSubsystemReturnsServiceUnavailableImmediately() {
    MessageDaemon daemon = mock(MessageDaemon.class, RETURNS_DEEP_STUBS);
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setStatus(Status.ERROR);
    when(daemon.getSubSystemManager().getSubSystemStatus())
        .thenReturn(List.of(status));

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      Response response = new ConsulHealth().getHealth();

      assertEquals(503, response.getStatus());
      assertEquals("Error", response.getEntity());
    }
  }
}
