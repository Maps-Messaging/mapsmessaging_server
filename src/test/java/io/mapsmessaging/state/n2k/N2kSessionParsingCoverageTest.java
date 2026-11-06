/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.n2k;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class N2kSessionParsingCoverageTest {

  private final N2kSession session = mock(N2kSession.class, CALLS_REAL_METHODS);

  @ParameterizedTest
  @MethodSource("jsonObjectCases")
  void getJsonObjectReturnsOnlyObjects(String json, String field, boolean expectedPresent)
      throws Exception {
    JsonObject root = json == null ? null : JsonParser.parseString(json).getAsJsonObject();

    JsonObject result = invoke("getJsonObject",
        new Class<?>[]{JsonObject.class, String.class}, root, field);

    assertEquals(expectedPresent, result != null);
  }

  @ParameterizedTest
  @MethodSource("integerCases")
  void getIntegerHandlesMissingNullAndNumericValues(String json, String field, Integer expected)
      throws Exception {
    JsonObject root = json == null ? null : JsonParser.parseString(json).getAsJsonObject();

    Integer result = invoke("getInteger",
        new Class<?>[]{JsonObject.class, String.class}, root, field);

    assertEquals(expected, result);
  }

  @ParameterizedTest
  @MethodSource("doubleCases")
  void getDoubleHandlesMissingNullAndNumericValues(String json, String field, Double expected)
      throws Exception {
    JsonObject root = json == null ? null : JsonParser.parseString(json).getAsJsonObject();

    Double result = invoke("getDouble",
        new Class<?>[]{JsonObject.class, String.class}, root, field);

    assertEquals(expected, result);
  }

  @ParameterizedTest
  @MethodSource("sourceCases")
  void sourceInstanceIdUsesCanSourceWhenPresent(
      String sourceName, String j1939Json, String expected) throws Exception {
    JsonObject j1939 = JsonParser.parseString(j1939Json).getAsJsonObject();

    String result = invoke("resolveSourceInstanceId",
        new Class<?>[]{String.class, JsonObject.class}, sourceName, j1939);

    assertEquals(expected, result);
  }

  @ParameterizedTest
  @MethodSource("sequenceCases")
  void sequenceNumberPrefersSequenceIdThenSid(
      String packetJson, Long expected) throws Exception {
    JsonObject packet = JsonParser.parseString(packetJson).getAsJsonObject();

    Long result = invoke("resolveSequenceNumber",
        new Class<?>[]{JsonObject.class}, packet);

    assertEquals(expected, result);
  }

  @ParameterizedTest
  @MethodSource("nameCases")
  void n2kNameResolutionHandlesMissingNullAndValues(String n2kJson, String expected)
      throws Exception {
    JsonObject n2k = n2kJson == null
        ? null
        : JsonParser.parseString(n2kJson).getAsJsonObject();

    String result = invoke("resolveN2kName",
        new Class<?>[]{JsonObject.class}, n2k);

    assertEquals(expected, result);
  }

  @Test
  void createContextPopulatesResolvedFields() throws Exception {
    JsonObject j1939 = JsonParser.parseString("{\"source\":23}").getAsJsonObject();
    JsonObject n2k = JsonParser.parseString("{\"name\":\"Vessel Heading\"}").getAsJsonObject();
    JsonObject packet = JsonParser.parseString("{\"sequenceId\":12.9}").getAsJsonObject();

    TwinUpdateContext context = invoke("createContext",
        new Class<?>[]{String.class, JsonObject.class, JsonObject.class, JsonObject.class},
        "/n2k/can0", j1939, n2k, packet);

    assertEquals("n2k-updater", context.getUpdateSource());
    assertEquals("/n2k/can0:source-23", context.getSourceInstanceId());
    assertEquals(12L, context.getSequenceNumber());
    assertEquals("Vessel Heading", context.getReason());
    assertFalse(context.isFullSnapshot());
    assertNotNull(context.getReceivedTime());
  }

  @Test
  void createContextFallsBackWhenOptionalFieldsAreAbsent() throws Exception {
    TwinUpdateContext context = invoke("createContext",
        new Class<?>[]{String.class, JsonObject.class, JsonObject.class, JsonObject.class},
        "/n2k/can0", new JsonObject(), new JsonObject(), new JsonObject());

    assertEquals("/n2k/can0", context.getSourceInstanceId());
    assertNull(context.getSequenceNumber());
    assertNull(context.getReason());
  }

  @SuppressWarnings("unchecked")
  private <T> T invoke(String name, Class<?>[] parameterTypes, Object... arguments)
      throws Exception {
    Method method = N2kSession.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(session, arguments);
  }

  private static Stream<Arguments> jsonObjectCases() {
    return Stream.of(
        Arguments.of(null, "j1939", false),
        Arguments.of("{}", "j1939", false),
        Arguments.of("{\"j1939\":null}", "j1939", false),
        Arguments.of("{\"j1939\":1}", "j1939", false),
        Arguments.of("{\"j1939\":\"x\"}", "j1939", false),
        Arguments.of("{\"j1939\":[]}", "j1939", false),
        Arguments.of("{\"j1939\":{}}", "j1939", true),
        Arguments.of("{\"a\":{},\"b\":{}}", "b", true)
    );
  }

  private static Stream<Arguments> integerCases() {
    return Stream.of(
        Arguments.of(null, "pgn", null),
        Arguments.of("{}", "pgn", null),
        Arguments.of("{\"pgn\":null}", "pgn", null),
        Arguments.of("{\"pgn\":0}", "pgn", 0),
        Arguments.of("{\"pgn\":127250}", "pgn", 127250),
        Arguments.of("{\"pgn\":-1}", "pgn", -1),
        Arguments.of("{\"pgn\":12.9}", "pgn", 12)
    );
  }

  private static Stream<Arguments> doubleCases() {
    return Stream.of(
        Arguments.of(null, "sid", null),
        Arguments.of("{}", "sid", null),
        Arguments.of("{\"sid\":null}", "sid", null),
        Arguments.of("{\"sid\":0}", "sid", 0.0),
        Arguments.of("{\"sid\":1.5}", "sid", 1.5),
        Arguments.of("{\"sid\":-2.25}", "sid", -2.25)
    );
  }

  private static Stream<Arguments> sourceCases() {
    return Stream.of(
        Arguments.of("/n2k/can0", "{}", "/n2k/can0"),
        Arguments.of("/n2k/can0", "{\"source\":null}", "/n2k/can0"),
        Arguments.of("/n2k/can0", "{\"source\":0}", "/n2k/can0:source-0"),
        Arguments.of("/n2k/can0", "{\"source\":23}", "/n2k/can0:source-23"),
        Arguments.of("topic", "{\"source\":255}", "topic:source-255")
    );
  }

  private static Stream<Arguments> sequenceCases() {
    return Stream.of(
        Arguments.of("{}", null),
        Arguments.of("{\"sequenceId\":null}", null),
        Arguments.of("{\"sid\":null}", null),
        Arguments.of("{\"sid\":7}", 7L),
        Arguments.of("{\"sequenceId\":5}", 5L),
        Arguments.of("{\"sequenceId\":5,\"sid\":7}", 5L),
        Arguments.of("{\"sequenceId\":5.9}", 5L),
        Arguments.of("{\"sid\":-1}", -1L)
    );
  }

  private static Stream<Arguments> nameCases() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of("{}", null),
        Arguments.of("{\"name\":null}", null),
        Arguments.of("{\"name\":\"\"}", ""),
        Arguments.of("{\"name\":\"Heading\"}", "Heading"),
        Arguments.of("{\"name\":\"Vessel Heading\"}", "Vessel Heading")
    );
  }
}
