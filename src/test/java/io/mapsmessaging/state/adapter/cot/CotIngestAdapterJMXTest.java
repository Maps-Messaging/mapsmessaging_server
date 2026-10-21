package io.mapsmessaging.state.adapter.cot;

import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CotIngestAdapterJMXTest {

  @Test
  void metricsDelegateToAdapter() {
    boolean original = JMXManager.isEnableJMX();
    try {
      JMXManager.setEnableJMX(false);
      CotIngestAdapter adapter = mock(CotIngestAdapter.class);
      when(adapter.getRoutedCount()).thenReturn(11L);
      when(adapter.getDroppedCount()).thenReturn(3L);
      when(adapter.getLastMessageAgeMillis()).thenReturn(50L);

      CotIngestAdapterJMX jmx = new CotIngestAdapterJMX(adapter);

      assertEquals(11L, jmx.getRoutedCount());
      assertEquals(3L, jmx.getDroppedCount());
      assertEquals(50L, jmx.getLastMessageAgeMillis());
      assertDoesNotThrow(jmx::close);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }
}
