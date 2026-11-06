/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.n2k;

import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class N2kTwinUpdaterIdentityCoverageTest {

  private final N2kTwinUpdater updater = mock(N2kTwinUpdater.class, CALLS_REAL_METHODS);

  @ParameterizedTest
  @MethodSource("twinIdCases")
  void buildTwinIdNormalizesNameOrTopic(String name, String topic, String expected)
      throws Exception {
    N2KTwinConfig config = config(name, topic, null);

    assertEquals(expected, invoke("buildTwinId",
        new Class<?>[]{N2KTwinConfig.class}, config));
  }

  @ParameterizedTest
  @MethodSource("normalizationCases")
  void normalizeTwinIdHandlesProtocolCharacters(String value, String expected)
      throws Exception {
    assertEquals(expected, invoke("normalizeTwinId",
        new Class<?>[]{String.class}, value));
  }

  @ParameterizedTest
  @MethodSource("vehicleClassCases")
  void resolveVehicleClassHandlesCaseWhitespaceAndUnknownValues(
      String configured, VehicleClass expected) throws Exception {
    N2KTwinConfig config = config("asset", "/topic", configured);

    assertEquals(expected, invoke("resolveVehicleClass",
        new Class<?>[]{N2KTwinConfig.class}, config));
  }

  @ParameterizedTest
  @MethodSource("identityFallbackCases")
  void displayDescriptionAndCallsignFollowNameFallbacks(
      String name, String twinId, String expectedDisplay, String expectedDescription,
      String expectedCallsign) throws Exception {
    N2KTwinConfig config = config(name, "/topic", null);

    assertEquals(expectedDisplay, invoke("resolveDisplayName",
        new Class<?>[]{String.class, N2KTwinConfig.class}, twinId, config));
    assertEquals(expectedDescription, invoke("resolveDescription",
        new Class<?>[]{String.class, N2KTwinConfig.class}, twinId, config));
    assertEquals(expectedCallsign, invoke("resolveCallSign",
        new Class<?>[]{String.class, N2KTwinConfig.class}, twinId, config));
  }

  @Test
  void responseTopicIsSetOnlyWhenCurrentValueIsMissing() throws Exception {
    DroneTwin twin = new DroneTwin("asset");

    invokeVoid("updateTwinResponseTopic",
        new Class<?>[]{io.mapsmessaging.state.drone.core.EntityTwin.class, String.class},
        twin, "/reply/one");
    assertEquals("/reply/one", twin.getResponseTopicName());

    invokeVoid("updateTwinResponseTopic",
        new Class<?>[]{io.mapsmessaging.state.drone.core.EntityTwin.class, String.class},
        twin, "/reply/two");
    assertEquals("/reply/one", twin.getResponseTopicName());
  }

  @ParameterizedTest
  @MethodSource("ignoredResponseTopics")
  void blankResponseTopicIsIgnored(String responseTopic) throws Exception {
    DroneTwin twin = new DroneTwin("asset");

    invokeVoid("updateTwinResponseTopic",
        new Class<?>[]{io.mapsmessaging.state.drone.core.EntityTwin.class, String.class},
        twin, responseTopic);

    assertNull(twin.getResponseTopicName());
  }

  @Test
  void resolveTimestampUsesReceivedTimeWhenProvided() throws Exception {
    Instant expected = Instant.parse("2026-10-03T01:02:03Z");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(expected);

    assertEquals(expected, invoke("resolveTimestamp",
        new Class<?>[]{TwinUpdateContext.class}, context));
  }

  @Test
  void resolveTimestampFallsBackToCurrentTime() throws Exception {
    TwinUpdateContext context = new TwinUpdateContext();
    Instant before = Instant.now();

    Instant resolved = invoke("resolveTimestamp",
        new Class<?>[]{TwinUpdateContext.class}, context);

    Instant after = Instant.now();
    assertFalse(resolved.isBefore(before));
    assertFalse(resolved.isAfter(after));
  }

  @SuppressWarnings("unchecked")
  private <T> T invoke(String name, Class<?>[] parameterTypes, Object... arguments)
      throws Exception {
    Method method = N2kTwinUpdater.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(updater, arguments);
  }

  private void invokeVoid(String name, Class<?>[] parameterTypes, Object... arguments)
      throws Exception {
    Method method = N2kTwinUpdater.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    method.invoke(updater, arguments);
  }

  private static N2KTwinConfig config(String name, String topic, String vehicleClass) {
    N2KTwinConfig config = new N2KTwinConfig();
    config.setName(name);
    config.setTopic(topic);
    config.setVehicleClass(vehicleClass);
    return config;
  }

  private static Stream<Arguments> twinIdCases() {
    return Stream.of(
        Arguments.of("asset", "/fallback", "asset"),
        Arguments.of("  asset  ", "/fallback", "asset"),
        Arguments.of("asset/one", "/fallback", "asset-one"),
        Arguments.of("asset///one", "/fallback", "asset-one"),
        Arguments.of("asset#one", "/fallback", "asset_one"),
        Arguments.of("asset+one", "/fallback", "asset_one"),
        Arguments.of(null, "/canbus0/n2k/json/#", "canbus0-n2k-json-_"),
        Arguments.of("", "/edge/+/feed/#", "edge-_-feed-_"),
        Arguments.of(" ", "/topic", "topic"),
        Arguments.of(null, null, "n2k"),
        Arguments.of("", "", "n2k")
    );
  }

  private static Stream<Arguments> normalizationCases() {
    return Stream.of(
        Arguments.of("asset", "asset"),
        Arguments.of(" asset ", "asset"),
        Arguments.of("/asset/", "asset"),
        Arguments.of("///asset///", "asset"),
        Arguments.of("a/b/c", "a-b-c"),
        Arguments.of("a//b///c", "a-b-c"),
        Arguments.of("#asset", "_asset"),
        Arguments.of("+asset", "_asset"),
        Arguments.of("asset#", "asset_"),
        Arguments.of("asset+", "asset_"),
        Arguments.of("a/#/+/b", "a-_-_-b"),
        Arguments.of("---asset---", "asset")
    );
  }

  private static Stream<Arguments> vehicleClassCases() {
    return Stream.of(
        Arguments.of(null, VehicleClass.USV),
        Arguments.of("", VehicleClass.USV),
        Arguments.of(" ", VehicleClass.USV),
        Arguments.of("USV", VehicleClass.USV),
        Arguments.of("uav", VehicleClass.UAV),
        Arguments.of(" ugv ", VehicleClass.UGV),
        Arguments.of("UUV", VehicleClass.UUV),
        Arguments.of("gcs", VehicleClass.GCS),
        Arguments.of("unknown", VehicleClass.UNKNOWN),
        Arguments.of("vessel", VehicleClass.USV),
        Arguments.of("rover", VehicleClass.USV),
        Arguments.of("garbage", VehicleClass.USV)
    );
  }

  private static Stream<Arguments> identityFallbackCases() {
    return Stream.of(
        Arguments.of("Boat One", "boat-one", "Boat One",
            "N2K STANAG feed Boat One", "Boat One"),
        Arguments.of(null, "boat-one", "boat-one",
            "N2K STANAG feed boat-one", "boat-one"),
        Arguments.of("", "boat-one", "boat-one",
            "N2K STANAG feed boat-one", "boat-one"),
        Arguments.of(" ", "boat-one", "boat-one",
            "N2K STANAG feed boat-one", "boat-one")
    );
  }

  private static Stream<Arguments> ignoredResponseTopics() {
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(""),
        Arguments.of(" ")
    );
  }
}
