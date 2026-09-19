/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.mavlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.utilities.admin.JMXManager;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

class MavlinkIntegrationJMXTest {

  @Test
  void subscriberBuildsStableSourceIdentityFromNameAndTopic() {
    MavlinkTwinConfigDTO config = new MavlinkTwinConfigDTO();
    config.setName("primary");
    config.setTopic("/mavlink/+/telemetry");

    assertEquals(
        "primary|/mavlink/+/telemetry",
        MavlinkStateSubscriber.integrationSource(config));
  }

  @Test
  void distinctSourcesCanRegisterAndCloseIndependently() throws Exception {
    boolean originalEnabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(true);
    MavlinkIntegrationJMX first = null;
    MavlinkIntegrationJMX second = null;
    try {
      first = new MavlinkIntegrationJMX(mock(MavlinkTwinUpdater.class), "alpha|/mavlink/alpha/#");
      second = new MavlinkIntegrationJMX(mock(MavlinkTwinUpdater.class), "bravo|/mavlink/bravo/#");

      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      ObjectName firstName = objectName("alpha|/mavlink/alpha/#");
      ObjectName secondName = objectName("bravo|/mavlink/bravo/#");

      assertTrue(server.isRegistered(firstName));
      assertTrue(server.isRegistered(secondName));

      first.close();
      first = null;

      assertFalse(server.isRegistered(firstName));
      assertTrue(server.isRegistered(secondName));
    } finally {
      if (first != null) {
        first.close();
      }
      if (second != null) {
        second.close();
      }
      JMXManager.setEnableJMX(originalEnabled);
    }
  }

  private ObjectName objectName(String source) throws Exception {
    return new ObjectName(
        "io.mapsmessaging:type=Integration,name=Mavlink,source=" + ObjectName.quote(source));
  }
}
