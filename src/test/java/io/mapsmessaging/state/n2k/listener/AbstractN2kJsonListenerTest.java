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

package io.mapsmessaging.state.n2k.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AbstractN2kJsonListenerTest {

  private final TestListener listener = new TestListener();

  @ParameterizedTest
  @MethodSource("unusableNumericValues")
  void getDouble_unusableValue_returnsNull(JsonElement value) {
    JsonObject packet = new JsonObject();
    if (value != null) {
      packet.add("value", value);
    }

    assertNull(listener.readDouble(packet, "value"));
  }

  static Stream<Arguments> unusableNumericValues() {
    JsonArray array = new JsonArray();
    array.add(1);
    return Stream.of(
        Arguments.of((JsonElement) null),
        Arguments.of(JsonNull.INSTANCE),
        Arguments.of(new JsonPrimitive(true)),
        Arguments.of(new JsonObject()),
        Arguments.of(array),
        Arguments.of(new JsonPrimitive("not-a-number")),
        Arguments.of(new JsonPrimitive("NaN")),
        Arguments.of(new JsonPrimitive("Infinity")),
        Arguments.of(new JsonPrimitive("-Infinity")));
  }

  @ParameterizedTest
  @MethodSource("usableDoubleValues")
  void getDouble_numericPrimitiveOrString_returnsValue(JsonElement value, double expected) {
    JsonObject packet = new JsonObject();
    packet.add("value", value);

    assertEquals(expected, listener.readDouble(packet, "value"));
  }

  static Stream<Arguments> usableDoubleValues() {
    return Stream.of(
        Arguments.of(new JsonPrimitive(0), 0.0d),
        Arguments.of(new JsonPrimitive(-12.75d), -12.75d),
        Arguments.of(new JsonPrimitive("42.5"), 42.5d));
  }

  @Test
  void getDouble_firstAbsentOrNullAlias_usesLaterAlias() {
    JsonObject packet = new JsonObject();
    packet.add("first", JsonNull.INSTANCE);
    packet.addProperty("second", "3.25");

    assertEquals(3.25d, listener.readDouble(packet, "missing", "first", "second"));
  }

  @ParameterizedTest
  @MethodSource("integerValues")
  void getInteger_onlyExactInRangeValuesAreAccepted(JsonElement value, Integer expected) {
    JsonObject packet = new JsonObject();
    packet.add("value", value);

    assertEquals(expected, listener.readInteger(packet, "value"));
  }

  static Stream<Arguments> integerValues() {
    return Stream.of(
        Arguments.of(new JsonPrimitive(0), 0),
        Arguments.of(new JsonPrimitive("2147483647"), Integer.MAX_VALUE),
        Arguments.of(new JsonPrimitive("-2147483648"), Integer.MIN_VALUE),
        Arguments.of(new JsonPrimitive("1.5"), null),
        Arguments.of(new JsonPrimitive("2147483648"), null),
        Arguments.of(new JsonPrimitive(true), null),
        Arguments.of(new JsonObject(), null));
  }

  @ParameterizedTest
  @MethodSource("longValues")
  void getLong_onlyExactInRangeValuesAreAccepted(JsonElement value, Long expected) {
    JsonObject packet = new JsonObject();
    packet.add("value", value);

    assertEquals(expected, listener.readLong(packet, "value"));
  }

  static Stream<Arguments> longValues() {
    return Stream.of(
        Arguments.of(new JsonPrimitive("0"), 0L),
        Arguments.of(new JsonPrimitive(Long.MAX_VALUE), Long.MAX_VALUE),
        Arguments.of(new JsonPrimitive("2.1"), null),
        Arguments.of(new JsonPrimitive("9223372036854775808"), null),
        Arguments.of(new JsonPrimitive(false), null));
  }

  @Test
  void getString_acceptsOnlyStringPrimitive() {
    JsonObject packet = new JsonObject();
    packet.addProperty("string", "alpha");
    packet.addProperty("number", 12);
    packet.addProperty("boolean", true);

    assertEquals("alpha", listener.readString(packet, "string"));
    assertNull(listener.readString(packet, "number"));
    assertNull(listener.readString(packet, "boolean"));
  }

  @Test
  void hasAny_distinguishesAbsentNullAndZero() {
    JsonObject packet = new JsonObject();
    packet.add("nullValue", JsonNull.INSTANCE);
    packet.addProperty("zero", 0);

    assertFalse(listener.any(null, "zero"));
    assertFalse(listener.any(packet, "missing", "nullValue"));
    assertTrue(listener.any(packet, "missing", "zero"));
  }

  @ParameterizedTest
  @MethodSource("latitudeValues")
  void latitudeValidation_appliesFiniteProtocolRange(Double latitude, boolean expected) {
    assertEquals(expected, listener.validLatitude(latitude));
  }

  static Stream<Arguments> latitudeValues() {
    return Stream.of(
        Arguments.of(null, false),
        Arguments.of(Double.NaN, false),
        Arguments.of(Double.POSITIVE_INFINITY, false),
        Arguments.of(-90.0d, true),
        Arguments.of(90.0d, true),
        Arguments.of(-90.0001d, false),
        Arguments.of(90.0001d, false));
  }

  @ParameterizedTest
  @MethodSource("longitudeValues")
  void longitudeValidation_appliesFiniteProtocolRange(Double longitude, boolean expected) {
    assertEquals(expected, listener.validLongitude(longitude));
  }

  static Stream<Arguments> longitudeValues() {
    return Stream.of(
        Arguments.of(null, false),
        Arguments.of(Double.NaN, false),
        Arguments.of(Double.NEGATIVE_INFINITY, false),
        Arguments.of(-180.0d, true),
        Arguments.of(180.0d, true),
        Arguments.of(-180.0001d, false),
        Arguments.of(180.0001d, false));
  }

  @ParameterizedTest
  @MethodSource("degreeValues")
  void normalizeDegrees_wrapsFiniteAngles(Double input, Double expected) {
    assertEquals(expected, listener.normalize(input));
  }

  static Stream<Arguments> degreeValues() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(Double.NaN, null),
        Arguments.of(Double.POSITIVE_INFINITY, null),
        Arguments.of(0.0d, 0.0d),
        Arguments.of(360.0d, 0.0d),
        Arguments.of(-90.0d, 270.0d),
        Arguments.of(810.0d, 90.0d));
  }

  @Test
  void resolveTimestamp_usesReceiveTimeFromContext() {
    Instant receivedTime = Instant.parse("2026-07-28T10:15:30Z");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(receivedTime);

    assertSame(receivedTime, listener.timestamp(context));
  }

  private static final class TestListener extends AbstractN2kJsonListener {

    @Override
    public int getPgn() {
      return 1;
    }

    @Override
    public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
    }

    private Double readDouble(JsonObject packet, String... names) {
      return getDouble(packet, names);
    }

    private Integer readInteger(JsonObject packet, String... names) {
      return getInteger(packet, names);
    }

    private Long readLong(JsonObject packet, String... names) {
      return getLong(packet, names);
    }

    private String readString(JsonObject packet, String... names) {
      return getString(packet, names);
    }

    private boolean any(JsonObject packet, String... names) {
      return hasAny(packet, names);
    }

    private boolean validLatitude(Double value) {
      return isValidLatitude(value);
    }

    private boolean validLongitude(Double value) {
      return isValidLongitude(value);
    }

    private Double normalize(Double value) {
      return normalizeDegrees(value);
    }

    private Instant timestamp(TwinUpdateContext context) {
      return resolveTimestamp(context);
    }
  }
}
