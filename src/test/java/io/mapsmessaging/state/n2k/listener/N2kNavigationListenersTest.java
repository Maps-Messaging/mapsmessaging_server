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

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class N2kNavigationListenersTest {

  private static final Instant RECEIVED_TIME = Instant.parse("2026-07-28T10:15:30Z");
  private static final double DELTA = 0.0000001d;

  @Test
  void heading_numericStringAndNegativeAngle_updatesConvertedValues() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("headingSensorReading", String.valueOf(-Math.PI / 2.0d));
    packet.addProperty("variation", Math.PI / 6.0d);
    packet.add("deviation", JsonNull.INSTANCE);
    packet.addProperty("headingSensorReference", 2);

    new N2kHeadingJsonListener().handle(droneTwin, packet, context());

    assertEquals(270.0d, droneTwin.getHeadingDegrees(), DELTA);
    assertEquals(30.0d, attributeDouble(droneTwin, "n2k.heading.variationDegrees"), DELTA);
    assertNull(droneTwin.getAttributes().get("n2k.heading.deviationDegrees"));
    assertEquals("2", droneTwin.getAttributes().get("n2k.heading.sensorReference"));
    assertSame(RECEIVED_TIME, droneTwin.getNavigationUpdatedAt());
    assertSame(RECEIVED_TIME, droneTwin.getLastSeenAt());
  }

  @Test
  void heading_malformedHeading_preservesUsableVariationAndDeviation() {
    DroneTwin droneTwin = new DroneTwin();
    droneTwin.setHeadingDegrees(45.0d);
    JsonObject packet = new JsonObject();
    packet.add("headingSensorReading", new JsonObject());
    packet.addProperty("variation", Math.PI / 4.0d);
    packet.addProperty("deviation", -Math.PI / 18.0d);

    new N2kHeadingJsonListener().handle(droneTwin, packet, context());

    assertEquals(45.0d, droneTwin.getHeadingDegrees());
    assertEquals(45.0d, attributeDouble(droneTwin, "n2k.heading.variationDegrees"), DELTA);
    assertEquals(-10.0d, attributeDouble(droneTwin, "n2k.heading.deviationDegrees"), DELTA);
  }

  @Test
  void motion_wrapsCourseAndPreservesZeroSpeedAndReference() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("courseOverGround", Math.toRadians(450.0d));
    packet.addProperty("speedOverGround", "0");
    packet.addProperty("cogReference", 1);

    new N2kMotionJsonListener().handle(droneTwin, packet, context());

    assertEquals(90.0d, droneTwin.getCourseOverGroundDegrees(), DELTA);
    assertEquals(0.0d, droneTwin.getGroundSpeedMetersPerSecond(), DELTA);
    assertEquals("1", droneTwin.getAttributes().get("n2k.motion.courseReference"));
    assertSame(RECEIVED_TIME, droneTwin.getOperationalUpdatedAt());
    assertSame(RECEIVED_TIME, droneTwin.getMotionUpdatedAt());
  }

  @Test
  void motion_malformedCourse_stillUpdatesSpeed() {
    DroneTwin droneTwin = new DroneTwin();
    droneTwin.setCourseOverGroundDegrees(12.0d);
    JsonObject packet = new JsonObject();
    packet.addProperty("courseOverGround", "not-a-number");
    packet.addProperty("speedOverGround", 4.25d);

    new N2kMotionJsonListener().handle(droneTwin, packet, context());

    assertEquals(12.0d, droneTwin.getCourseOverGroundDegrees());
    assertEquals(4.25d, droneTwin.getGroundSpeedMetersPerSecond());
  }

  @ParameterizedTest
  @MethodSource("invalidPositions")
  void rapidPosition_invalidOrPartialCoordinates_doNotOverwritePosition(Double latitude, Double longitude) {
    DroneTwin droneTwin = new DroneTwin();
    GeoPosition existing = new GeoPosition(-33.8d, 151.2d, null, null);
    droneTwin.setGeoPosition(existing);
    droneTwin.setGpsValid(false);
    JsonObject packet = new JsonObject();
    if (latitude != null) {
      packet.addProperty("latitude", latitude);
    }
    if (longitude != null) {
      packet.addProperty("longitude", longitude);
    }

    new N2kPositionJsonListener().handle(droneTwin, packet, context());

    assertSame(existing, droneTwin.getGeoPosition());
    assertFalse(droneTwin.getGpsValid());
    assertNull(droneTwin.getNavigationUpdatedAt());
  }

  static Stream<Arguments> invalidPositions() {
    return Stream.of(
        Arguments.of(null, 151.2d),
        Arguments.of(-33.8d, null),
        Arguments.of(-90.0001d, 151.2d),
        Arguments.of(90.0001d, 151.2d),
        Arguments.of(-33.8d, -180.0001d),
        Arguments.of(-33.8d, 180.0001d),
        Arguments.of(Double.NaN, 151.2d),
        Arguments.of(-33.8d, Double.POSITIVE_INFINITY));
  }

  @Test
  void rapidPosition_validBoundaryCoordinates_updatesPosition() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("latitude", -90.0d);
    packet.addProperty("longitude", 180.0d);

    new N2kPositionJsonListener().handle(droneTwin, packet, context());

    assertEquals(-90.0d, droneTwin.getGeoPosition().getLatitude());
    assertEquals(180.0d, droneTwin.getGeoPosition().getLongitude());
    assertNull(droneTwin.getGeoPosition().getAltitudeMslMeters());
    assertTrue(droneTwin.getGpsValid());
  }

  @Test
  void gnssPosition_validCoordinates_preservesAltitudeInMetres() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("latitude", "-33.8688");
    packet.addProperty("longitude", "151.2093");
    packet.addProperty("altitude", "12.75");

    new N2kGnssJsonListener().handle(droneTwin, packet, context());

    assertEquals(-33.8688d, droneTwin.getGeoPosition().getLatitude());
    assertEquals(151.2093d, droneTwin.getGeoPosition().getLongitude());
    assertEquals(12.75d, droneTwin.getGeoPosition().getAltitudeMslMeters());
    assertTrue(droneTwin.getGpsValid());
    assertSame(RECEIVED_TIME, droneTwin.getNavigationUpdatedAt());
  }

  @Test
  void gnssDops_malformedHdop_stillUpdatesVdopAndMetadata() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.add("hdop", new JsonObject());
    packet.addProperty("vdop", "1.25");
    packet.addProperty("tdop", 2.5d);
    packet.addProperty("setMode", 3);
    packet.addProperty("opMode", 2);

    new N2kGnssDopsJsonListener().handle(droneTwin, packet, context());

    assertNull(droneTwin.getFixInfo().getHdop());
    assertEquals(1.25d, droneTwin.getFixInfo().getVdop());
    assertEquals("2.5", droneTwin.getAttributes().get("n2k.gnss.tdop"));
    assertEquals("3", droneTwin.getAttributes().get("n2k.gnss.setMode"));
    assertEquals("2", droneTwin.getAttributes().get("n2k.gnss.operationMode"));
    assertTrue(droneTwin.getGpsValid());
  }

  @Test
  void attitude_partialAndMalformedFields_preservesNullForUnusableAxis() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("roll", "bad");
    packet.addProperty("pitch", Math.PI / 6.0d);
    packet.addProperty("yaw", -Math.PI / 2.0d);

    new N2kAttitudeJsonListener().handle(droneTwin, packet, context());

    assertNull(droneTwin.getOrientation().getRollDegrees());
    assertEquals(30.0d, droneTwin.getOrientation().getPitchDegrees(), DELTA);
    assertEquals(-90.0d, droneTwin.getOrientation().getYawDegrees(), DELTA);
    assertSame(RECEIVED_TIME, droneTwin.getMotionUpdatedAt());
  }

  @Test
  void rateOfTurn_convertsRadiansPerSecondToDegreesPerSecond() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("rateOfTurn", Math.PI / 3.0d);

    new N2kRateOfTurnJsonListener().handle(droneTwin, packet, context());

    assertEquals(60.0d, attributeDouble(droneTwin, "n2k.rateOfTurnDegreesPerSecond"), DELTA);
    assertSame(RECEIVED_TIME, droneTwin.getMotionUpdatedAt());
  }

  @Test
  void wind_convertsDirectionAndPreservesZeroSpeed() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("windSpeed", 0.0d);
    packet.addProperty("windDirection", Math.PI);
    packet.addProperty("windReference", 4);

    new N2kWindJsonListener().handle(droneTwin, packet, context());

    assertEquals(0.0d, attributeDouble(droneTwin, "n2k.windSpeedMetersPerSecond"));
    assertEquals(180.0d, attributeDouble(droneTwin, "n2k.windDirectionDegrees"), DELTA);
    assertEquals("4", droneTwin.getAttributes().get("n2k.windReference"));
    assertSame(RECEIVED_TIME, droneTwin.getOperationalUpdatedAt());
  }

  @Test
  void magneticVariation_malformedVariation_stillStoresSourceAndAge() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("variation", "unknown");
    packet.addProperty("variationSource", 5);
    packet.addProperty("ageOfServiceDate", 20_300);

    new N2kMagneticVariationJsonListener().handle(droneTwin, packet, context());

    assertNull(droneTwin.getAttributes().get("n2k.magneticVariationDegrees"));
    assertEquals("5", droneTwin.getAttributes().get("n2k.magneticVariationSource"));
    assertEquals("20300", droneTwin.getAttributes().get("n2k.magneticVariationAgeOfServiceDate"));
    assertSame(RECEIVED_TIME, droneTwin.getNavigationUpdatedAt());
  }

  @Test
  void emptyOrNullOnlyPacket_doesNotAdvanceTimestamps() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.add("courseOverGround", JsonNull.INSTANCE);

    new N2kMotionJsonListener().handle(droneTwin, packet, context());

    assertNull(droneTwin.getMotionUpdatedAt());
    assertNull(droneTwin.getOperationalUpdatedAt());
    assertNull(droneTwin.getLastSeenAt());
  }

  private static TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(RECEIVED_TIME);
    return context;
  }

  private static double attributeDouble(DroneTwin droneTwin, String key) {
    return Double.parseDouble(droneTwin.getAttributes().get(key));
  }
}
