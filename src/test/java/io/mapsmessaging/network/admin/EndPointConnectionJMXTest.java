package io.mapsmessaging.network.admin;

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EndPointConnectionJMXTest {

  @Test
  void managementOperationsDelegateToConnection() {
    boolean original = JMXManager.isEnableJMX();
    try {
      JMXManager.setEnableJMX(false);
      EndPointConnection connection = mock(EndPointConnection.class);
      EndPointConnectionJMX jmx =
          new EndPointConnectionJMX(List.of("test=connection"), connection);

      jmx.pauseConnection();
      jmx.resumeConnection();
      jmx.stopConnection();
      jmx.startConnection();

      verify(connection).pause();
      verify(connection).resume();
      verify(connection).stop();
      verify(connection).start();

      HealthStatus health = jmx.checkHealth();
      assertNotNull(health);
      assertDoesNotThrow(jmx::close);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }
}
