package io.mapsmessaging.state.n2k;

import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.TwinManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class N2kTwinUpdaterBranchCoverageTest {

  @Test
  void twinIdPrefersNameThenTopicThenDefaultAndNormalizesSeparators() throws Exception {
    N2kTwinUpdater updater = new N2kTwinUpdater(mock(TwinManager.class));
    Method build = N2kTwinUpdater.class.getDeclaredMethod("buildTwinId", N2KTwinConfig.class);
    build.setAccessible(true);

    N2KTwinConfig config = new N2KTwinConfig();
    config.setName("  boat/a##+b  ");
    config.setTopic("/ignored/#");
    assertEquals("boat-a__b", build.invoke(updater, config));

    config.setName(" ");
    config.setTopic("/n2k/boat/#");
    assertEquals("n2k-boat-_", build.invoke(updater, config));

    config.setTopic(" ");
    assertEquals("n2k", build.invoke(updater, config));
  }

  @Test
  void vehicleClassDefaultsToUsvForBlankOrUnknownValues() throws Exception {
    N2kTwinUpdater updater = new N2kTwinUpdater(mock(TwinManager.class));
    Method resolve = N2kTwinUpdater.class.getDeclaredMethod(
        "resolveVehicleClass", N2KTwinConfig.class);
    resolve.setAccessible(true);

    N2KTwinConfig config = new N2KTwinConfig();
    config.setVehicleClass(null);
    assertEquals(VehicleClass.USV, resolve.invoke(updater, config));

    config.setVehicleClass("not-real");
    assertEquals(VehicleClass.USV, resolve.invoke(updater, config));

    config.setVehicleClass(" uav ");
    assertEquals(VehicleClass.UAV, resolve.invoke(updater, config));
  }

  @Test
  void responseTopicOnlyFillsAnEmptyTwinValue() throws Exception {
    N2kTwinUpdater updater = new N2kTwinUpdater(mock(TwinManager.class));
    Method update = N2kTwinUpdater.class.getDeclaredMethod(
        "updateTwinResponseTopic",
        io.mapsmessaging.state.drone.core.EntityTwin.class,
        String.class);
    update.setAccessible(true);

    io.mapsmessaging.state.drone.drone.DroneTwin twin =
        new io.mapsmessaging.state.drone.drone.DroneTwin("n2k");

    update.invoke(updater, twin, null);
    update.invoke(updater, twin, " ");
    assertNull(twin.getResponseTopicName());

    update.invoke(updater, twin, "/reply");
    assertEquals("/reply", twin.getResponseTopicName());

    update.invoke(updater, twin, "/other");
    assertEquals("/reply", twin.getResponseTopicName());
  }
}