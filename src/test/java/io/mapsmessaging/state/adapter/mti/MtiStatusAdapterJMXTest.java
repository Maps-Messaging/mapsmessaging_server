package io.mapsmessaging.state.adapter.mti;

import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MtiStatusAdapterJMXTest {

  @Test
  void metricsDelegateToAdapter() {
    boolean original = JMXManager.isEnableJMX();
    try {
      JMXManager.setEnableJMX(false);
      MtiStatusAdapter adapter = mock(MtiStatusAdapter.class);
      when(adapter.getCacheSize()).thenReturn(4);
      when(adapter.getUpsertCount()).thenReturn(10L);
      when(adapter.getDeleteCount()).thenReturn(2L);
      when(adapter.getLookupHitCount()).thenReturn(7L);
      when(adapter.getLookupMissCount()).thenReturn(3L);
      when(adapter.getLastMessageAgeMillis()).thenReturn(25L);
      when(adapter.getDegradedAssetCount()).thenReturn(1);
      when(adapter.getReadinessRate()).thenReturn(0.75d);

      MtiStatusAdapterJMX jmx = new MtiStatusAdapterJMX(adapter);

      assertEquals(4, jmx.getCacheSize());
      assertEquals(10L, jmx.getUpsertCount());
      assertEquals(2L, jmx.getDeleteCount());
      assertEquals(7L, jmx.getLookupHitCount());
      assertEquals(3L, jmx.getLookupMissCount());
      assertEquals(25L, jmx.getLastMessageAgeMillis());
      assertEquals(1, jmx.getDegradedAssetCount());
      assertEquals(0.75d, jmx.getReadinessRate(), 0.0d);
      assertDoesNotThrow(jmx::close);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }
}
