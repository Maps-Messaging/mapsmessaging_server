package io.mapsmessaging.utilities.admin;

import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DetailedStatisticsJMXTest {

  @Test
  void exposesSummaryStatisticsValues() {
    boolean original = JMXManager.isEnableJMX();
    try {
      JMXManager.setEnableJMX(false);
      SummaryStatistics stats = new SummaryStatistics();
      stats.addValue(1);
      stats.addValue(2);
      stats.addValue(3);

      LinkedMovingAverages moving = mock(LinkedMovingAverages.class);
      when(moving.getDetailedStatistics()).thenReturn(stats);

      DetailedStatisticsJMX jmx = new DetailedStatisticsJMX(List.of("test=detail"), moving);

      assertEquals(3.0, jmx.getMax(), 0.0);
      assertEquals(1.0, jmx.getMin(), 0.0);
      assertEquals(2.0, jmx.getMean(), 0.0);
      assertEquals(3L, jmx.getN());
      assertTrue(jmx.getVariance() > 0.0);
      assertDoesNotThrow(jmx::close);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }

  @Test
  void missingDetailedStatisticsReturnsNeutralValues() {
    boolean original = JMXManager.isEnableJMX();
    try {
      JMXManager.setEnableJMX(false);
      LinkedMovingAverages moving = mock(LinkedMovingAverages.class);
      when(moving.getDetailedStatistics()).thenReturn(null);

      DetailedStatisticsJMX jmx = new DetailedStatisticsJMX(List.of("test=empty"), moving);

      assertEquals(0.0, jmx.getMax(), 0.0);
      assertEquals(0.0, jmx.getMean(), 0.0);
      assertEquals(0L, jmx.getN());
      assertDoesNotThrow(jmx::close);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }
}
