/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.mavlink;

import static org.junit.jupiter.api.Assertions.*;
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
    MavlinkTwinConfigDTO first = new MavlinkTwinConfigDTO();
    first.setTopic("/mavlink/alpha/#");
    MavlinkTwinConfigDTO second = new MavlinkTwinConfigDTO();
    second.setTopic("/mavlink/bravo/#");

    assertEquals(
        "mavlink|/mavlink/alpha/#",
        MavlinkStateSubscriber.integrationSource(first));
    assertEquals(
        "mavlink|/mavlink/bravo/#",
        MavlinkStateSubscriber.integrationSource(second));
  }

  @Test
  void distinctSourcesCanRegisterAndCloseIndependently() {
    boolean originalEnabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(true);

    MavlinkIntegrationJMX first = null;
    MavlinkIntegrationJMX second = null;

    try {
      first = new MavlinkIntegrationJMX(
          mock(MavlinkTwinUpdater.class),
          "alpha|/mavlink/alpha/#");

      second = new MavlinkIntegrationJMX(
          mock(MavlinkTwinUpdater.class),
          "bravo|/mavlink/bravo/#");

      ObjectName firstName = first.getObjectName();
      ObjectName secondName = second.getObjectName();

      assertNotNull(firstName);
      assertNotNull(secondName);
      assertNotEquals(firstName, secondName);

      MBeanServer server = ManagementFactory.getPlatformMBeanServer();

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
