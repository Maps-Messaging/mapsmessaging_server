/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.BatteryState;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class TakTwinObserverStatsBranchCoverageTest {

  @Test
  void batteryStatsClampRoundAndReportChargingState() throws Exception {
    withObserver(observer -> {
      Method buildStats = TakTwinObserver.class.getDeclaredMethod("buildStats", BatteryState.class);
      buildStats.setAccessible(true);

      assertNull(buildStats.invoke(observer, new Object[] {null}));
      assertNull(buildStats.invoke(observer, new BatteryState()));

      BatteryState charging = new BatteryState();
      charging.setPercentage(78.6);
      charging.setTemperatureCelsius(34.4);
      charging.setCharging(true);
      assertEquals(
          "<stats battery=\"79\" battery_temp=\"34\" battery_status=\"Charging\"/>",
          buildStats.invoke(observer, charging)
      );

      BatteryState clamped = new BatteryState();
      clamped.setPercentage(120.0);
      clamped.setTemperatureCelsius(Double.POSITIVE_INFINITY);
      clamped.setCharging(false);
      assertEquals(
          "<stats battery=\"100\" battery_status=\"Discharging\"/>",
          buildStats.invoke(observer, clamped)
      );
    });
  }

  @Test
  void statsAreInsertedBeforeDetailEndAndRateLimited() throws Exception {
    withObserver(observer -> {
      Method appendStats =
          TakTwinObserver.class.getDeclaredMethod("appendStatsIfDue", EntityTwin.class, String.class);
      appendStats.setAccessible(true);

      DroneTwin twin = positionedTwin();
      BatteryState battery = new BatteryState();
      battery.setPercentage(50.0);
      twin.setBatteryState(battery);
      String xml = "<event><detail><contact/></detail></event>";

      String first = (String) appendStats.invoke(observer, twin, xml);
      assertEquals("<event><detail><contact/><stats battery=\"50\"/></detail></event>", first);

      String second = (String) appendStats.invoke(observer, twin, xml);
      assertEquals(xml, second);
    });
  }

  @Test
  void statsLeaveInvalidOrUnusableXmlUntouched() throws Exception {
    withObserver(observer -> {
      Method appendStats =
          TakTwinObserver.class.getDeclaredMethod("appendStatsIfDue", EntityTwin.class, String.class);
      appendStats.setAccessible(true);

      DroneTwin twin = positionedTwin();
      BatteryState battery = new BatteryState();
      battery.setPercentage(Double.NaN);
      battery.setTemperatureCelsius(Double.NaN);
      twin.setBatteryState(battery);

      assertNull(appendStats.invoke(observer, twin, null));
      assertEquals("", appendStats.invoke(observer, twin, ""));
      assertEquals("<event/>", appendStats.invoke(observer, twin, "<event/>"));

      battery.setPercentage(25.0);
      assertEquals("<event/>", appendStats.invoke(observer, twin, "<event/>"));
    });
  }

  private DroneTwin positionedTwin() {
    DroneTwin twin = new DroneTwin("asset");
    twin.setGeoPosition(new GeoPosition(38.4, -9.1, 125.0, null, null));
    twin.setLastSeenAt(Instant.now());
    return twin;
  }

  private void withObserver(ObserverAction action) throws Exception {
    boolean enabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    try (MockedStatic<ConfigurationManager> mocked = mockStatic(ConfigurationManager.class)) {
      mocked.when(ConfigurationManager::getInstance).thenReturn(configurationManager);
      TakTwinObserver observer = new TakTwinObserver(new TwinManager());
      try {
        action.run(observer);
      } finally {
        observer.shutdown();
      }
    } finally {
      JMXManager.setEnableJMX(enabled);
    }
  }

  private interface ObserverAction {
    void run(TakTwinObserver observer) throws Exception;
  }
}
