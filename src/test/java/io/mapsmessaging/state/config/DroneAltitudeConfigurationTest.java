/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.configuration.ConfigurationProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DroneAltitudeConfigurationTest {

  @Test
  void configuredAltitude_roundTripPreservesModeAndValue()
      throws ReflectiveOperationException {
    ConfigurationProperties root = rootWithDrone();
    ConfigurationProperties drone = drone(root);
    drone.put("altitudeMode", "fixed");
    drone.put("altitudeMeters", 7.5d);

    TwinManagerConfig loaded = newTwinManagerConfig(root);
    DroneInfoDTO loadedDrone = loaded.getDroneInfo().get(0);
    assertEquals(AltitudeMode.FIXED, loadedDrone.getAltitudeMode());
    assertEquals(7.5d, loadedDrone.getAltitudeMeters());

    TwinManagerConfig reloaded =
        newTwinManagerConfig(loaded.toConfigurationProperties());
    assertEquals(AltitudeMode.FIXED, reloaded.getDroneInfo().get(0).getAltitudeMode());
    assertEquals(7.5d, reloaded.getDroneInfo().get(0).getAltitudeMeters());
  }

  @Test
  void omittedAltitudeConfiguration_remainsOmitted()
      throws ReflectiveOperationException {
    TwinManagerConfig config = newTwinManagerConfig(rootWithDrone());

    assertNull(config.getDroneInfo().get(0).getAltitudeMode());
    assertNull(config.getDroneInfo().get(0).getAltitudeMeters());

    ConfigurationProperties saved = config.toConfigurationProperties();
    List<?> droneInfos = assertInstanceOf(List.class, saved.get("droneInfo"));
    ConfigurationProperties savedDrone =
        assertInstanceOf(ConfigurationProperties.class, droneInfos.get(0));
    assertNull(savedDrone.get("altitudeMode"));
    assertNull(savedDrone.get("altitudeMeters"));
  }

  @Test
  void unknownAltitudeMode_isRejected() {
    ConfigurationProperties root = rootWithDrone();
    drone(root).put("altitudeMode", "sometimes");

    InvocationTargetException exception =
        assertThrows(
            InvocationTargetException.class,
            () -> newTwinManagerConfig(root));
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
  }

  @Test
  void negativeAltitude_isPreserved() throws ReflectiveOperationException {
    ConfigurationProperties root = rootWithDrone();
    drone(root).put("altitudeMode", "FIXED");
    drone(root).put("altitudeMeters", -10.0d);

    TwinManagerConfig config = newTwinManagerConfig(root);

    assertEquals(-10.0d, config.getDroneInfo().get(0).getAltitudeMeters());
  }

  @Test
  void nonFiniteAltitude_isRejected() {
    ConfigurationProperties root = rootWithDrone();
    drone(root).put("altitudeMode", "FIXED");
    drone(root).put("altitudeMeters", Double.NaN);

    InvocationTargetException exception =
        assertThrows(
            InvocationTargetException.class,
            () -> newTwinManagerConfig(root));
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
  }

  private ConfigurationProperties rootWithDrone() {
    ConfigurationProperties root = new ConfigurationProperties();
    ConfigurationProperties drone = new ConfigurationProperties();
    drone.put("name", "stickleback");
    drone.put("uuid", UUID.randomUUID().toString());
    root.put("droneInfo", List.of(drone));
    return root;
  }

  private ConfigurationProperties drone(ConfigurationProperties root) {
    return assertInstanceOf(
        ConfigurationProperties.class,
        assertInstanceOf(List.class, root.get("droneInfo")).get(0));
  }

  private TwinManagerConfig newTwinManagerConfig(ConfigurationProperties properties)
      throws ReflectiveOperationException {
    Constructor<TwinManagerConfig> constructor =
        TwinManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }
}
