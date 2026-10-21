package io.mapsmessaging.utilities.admin;

import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import org.junit.jupiter.api.Test;

import javax.management.AttributeList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LinkedMovingAveragesJMXTest {

  @Test
  void attributesDelegateToMovingAverageSourceAndResetIsInvoked() {
    boolean original = JMXManager.isEnableJMX();
    try {
      JMXManager.setEnableJMX(false);
      LinkedMovingAverages moving = mock(LinkedMovingAverages.class);
      when(moving.getName()).thenReturn("traffic");
      when(moving.getNames()).thenReturn(new String[]{"1m", "5m"});
      when(moving.getTotal()).thenReturn(42L);
      when(moving.getUnits()).thenReturn("bytes");
      when(moving.getAverage("1m")).thenReturn(12L);

      LinkedMovingAveragesJMX jmx =
          new LinkedMovingAveragesJMX(List.of("test=averages"), moving);

      assertEquals(42L, jmx.getAttribute("total"));
      assertEquals("bytes", jmx.getAttribute("units"));
      assertEquals(12L, jmx.getAttribute("1m"));

      jmx.invoke("reset", new Object[0], new String[0]);
      verify(moving).reset();

      assertNotNull(jmx.getMBeanInfo());
      assertDoesNotThrow(jmx::close);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }

  @Test
  void bulkReadsReturnOneEntryPerRequestedAttribute() {
    boolean original = JMXManager.isEnableJMX();
    try {
      JMXManager.setEnableJMX(false);
      LinkedMovingAverages moving = mock(LinkedMovingAverages.class);
      when(moving.getName()).thenReturn("traffic2");
      when(moving.getNames()).thenReturn(new String[0]);
      when(moving.getTotal()).thenReturn(1L);
      when(moving.getUnits()).thenReturn("packets");

      LinkedMovingAveragesJMX jmx =
          new LinkedMovingAveragesJMX(List.of("test=bulk"), moving);

      AttributeList list = jmx.getAttributes(new String[]{"total", "units"});
      assertEquals(2, list.size());
      assertTrue(jmx.setAttributes(new AttributeList()).isEmpty());
      assertDoesNotThrow(jmx::close);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }
}
