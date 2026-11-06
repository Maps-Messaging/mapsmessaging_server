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

package io.mapsmessaging.state.kpi;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.state.metrics.FeedActivityRegistry;
import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Prometheus exporter rules (ic26-demo-shine observability/maps/prometheus.yml) match on the
 * exact key order and attribute names below - a renamed key or attribute silently drops a metric.
 */
class KpiJmxRegistrationTest {

  @TempDir
  Path dataDirectory;

  private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

  private Set<String> attributes(ObjectName name) throws Exception {
    Set<String> names = new TreeSet<>();
    for (MBeanAttributeInfo info : server.getMBeanInfo(name).getAttributes()) {
      names.add(info.getName());
    }
    return names;
  }

  /** MAPS joins keys with ", ", so the keys after the first carry a leading space - hence `,\\s*` in the rules. */
  private ObjectName only(String bean) throws Exception {
    List<ObjectName> names = new ArrayList<>();
    for (ObjectName name : server.queryNames(new ObjectName("io.mapsmessaging:*"), null)) {
      if (name.getKeyPropertyListString().contains("name=" + bean + ",") || name.getKeyPropertyListString().endsWith("name=" + bean)) {
        names.add(name);
      }
    }
    assertEquals(1, names.size(), bean);
    return names.get(0);
  }

  @Test
  void beans_useTheNamesTheExporterRulesExpect() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-10-01T10:00:00Z"));
    KpiConfig config = KpiConfig.from(new ConfigurationProperties(Map.of("dataDirectory", dataDirectory.toString())));
    long lastMessage = clock.millis();
    KpiEvaluator evaluator = new KpiEvaluator(config, new KpiEvaluator.Sources(
        List::of, uid -> null, uid -> null,
        () -> Map.of("mavlink:fleet", new FeedActivityRegistry.FeedState(lastMessage, null)), () -> false),
        clock, new KpiEventLog(dataDirectory), new KpiStateStore(dataDirectory));
    evaluator.start();
    clock.advanceSeconds(5);
    evaluator.evaluate();

    List<KpiBean> beans = new ArrayList<>();
    boolean jmxWasEnabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(true);
    try {
      beans.add(new KpiJMX(evaluator, KpiId.READINESS));
      beans.add(new KpiPictureJMX(evaluator));
      beans.add(new DurationJMX(List.of("type=Integration", "name=FaultRun", "failureType=FEED_LOSS", "stage=DETECTION"),
          evaluator.getRuns().statsFor("FEED_LOSS").stage(FaultRunTracker.Stage.DETECTION)));
      beans.add(new FaultRunsJMX("ALL", evaluator.getRuns().statsFor("ALL")));
      beans.add(new FeedOutageJMX(evaluator.getFeedMonitor().getFeeds().get(0), clock));

      ObjectName kpi = only("Kpi");
      assertEquals("type=Integration, name=Kpi, kpi=READINESS", kpi.getKeyPropertyListString());
      assertEquals(Set.of("Value", "Numerator", "Denominator", "Raw Band", "Band", "Critical", "Green Threshold",
          "Amber Threshold"), attributes(kpi));
      assertEquals(0.9, server.getAttribute(kpi, "Green Threshold"));
      assertEquals(-1.0, server.getAttribute(kpi, "Value"), "empty roster exports -1, not a vacuous 100%");

      ObjectName picture = only("KpiPicture");
      assertEquals("type=Integration, name=KpiPicture", picture.getKeyPropertyListString());
      assertTrue(attributes(picture).containsAll(Set.of("Overall Band", "Usable", "Fully Restored", "Roster Size",
          "Unknown No Mti Count", "Cache Rebuild In Progress")));

      assertEquals("type=Integration, name=FaultRun, failureType=FEED_LOSS, stage=DETECTION",
          only("FaultRun").getKeyPropertyListString());
      assertEquals(Set.of("Count", "Sum Seconds", "Last Seconds", "Max Seconds", "Not Reached Count"),
          attributes(only("FaultRun")));
      assertEquals("type=Integration, name=FaultRuns, failureType=ALL",
          only("FaultRuns").getKeyPropertyListString());

      ObjectName feed = only("FeedOutage");
      assertEquals("type=Integration, name=FeedOutage, kind=mavlink, feed=fleet", feed.getKeyPropertyListString());
      assertEquals(Set.of("In Outage", "Current Outage Seconds", "Outage Count", "Total Outage Seconds",
          "Last Outage Seconds", "Max Outage Seconds", "Expected Interval Seconds"), attributes(feed));
      assertEquals(1, server.getAttribute(feed, "In Outage"));
    } finally {
      beans.forEach(KpiBean::close);
      JMXManager.setEnableJMX(jmxWasEnabled);
    }
    assertTrue(server.queryNames(new ObjectName("io.mapsmessaging:*"), null).stream()
        .noneMatch(name -> name.getKeyPropertyListString().contains("name=Kpi")), "beans unregister");
  }

  @Test
  void feedNamesWithSpecialCharacters_areSanitised() {
    assertEquals("central_8089", FeedOutageJMX.sanitise("central:8089"));
    assertEquals("Aircraft_01", FeedOutageJMX.sanitise("Aircraft 01"));
    assertEquals("_", FeedOutageJMX.sanitise(""));
  }
}
