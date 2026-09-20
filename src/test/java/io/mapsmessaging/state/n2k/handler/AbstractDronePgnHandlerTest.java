package io.mapsmessaging.state.n2k.handler;

import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AbstractDronePgnHandlerTest {

  private final TestHandler handler = new TestHandler();

  @Test
  void activePositionRequiresLifecycleGpsPositionAndNavigationTimestamp() {
    assertFalse(handler.active(null));

    DroneTwin twin = activeTwin();
    assertTrue(handler.active(twin));

    twin.setLifecycleStatus(TwinLifecycleStatus.DISCONNECTED);
    assertFalse(handler.active(twin));

    twin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
    twin.setGpsValid(false);
    assertFalse(handler.active(twin));

    twin.setGpsValid(true);
    twin.setNavigationUpdatedAt(null);
    assertFalse(handler.active(twin));

    twin.setNavigationUpdatedAt(Instant.EPOCH);
    twin.getGeoPosition().setLatitude(null);
    assertFalse(handler.active(twin));
  }

  @Test
  void firstMotionObservationIsAlwaysMaterial() {
    assertTrue(handler.material(activeTwin(), new PgnEmissionState()));
  }

  @Test
  void unchangedSnapshotIsNotMaterial() {
    DroneTwin twin = activeTwin();
    PgnEmissionState state = new PgnEmissionState();

    handler.snapshot(twin, state);

    assertFalse(handler.material(twin, state));
  }

  @Test
  void movementHeadingCourseAndSpeedThresholdsTriggerMaterialChange() {
    DroneTwin twin = activeTwin();
    PgnEmissionState state = new PgnEmissionState();
    handler.snapshot(twin, state);

    twin.getGeoPosition().setLatitude(38.0001);
    assertTrue(handler.material(twin, state));

    twin = activeTwin();
    state = new PgnEmissionState();
    handler.snapshot(twin, state);
    twin.setHeadingDegrees(15.0);
    assertTrue(handler.material(twin, state));

    twin = activeTwin();
    state = new PgnEmissionState();
    handler.snapshot(twin, state);
    twin.setCourseOverGroundDegrees(25.0);
    assertTrue(handler.material(twin, state));

    twin = activeTwin();
    state = new PgnEmissionState();
    handler.snapshot(twin, state);
    twin.setGroundSpeedMetersPerSecond(1.5);
    assertTrue(handler.material(twin, state));
  }

  @Test
  void angularDifferenceHandlesWrapAroundAndNulls() {
    assertEquals(2.0, handler.angular(359.0, 1.0), 0.0);
    assertEquals(5.0, handler.angular(10.0, 15.0), 0.0);
    assertEquals(Double.MAX_VALUE, handler.angular(null, 10.0));
    assertEquals(Double.MAX_VALUE, handler.absolute(null, 10.0));
  }

  @Test
  void distanceAndSnapshotHelpersPreserveExpectedValues() {
    assertEquals(0.0, handler.distance(38.0, -9.0, 38.0, -9.0), 0.0);
    assertTrue(handler.distance(38.0, -9.0, 38.001, -9.0) > 100.0);

    DroneTwin twin = activeTwin();
    PgnEmissionState state = new PgnEmissionState();
    handler.snapshot(twin, state);

    assertEquals(twin.getGeoPosition().getLatitude(), state.getLastLatitude());
    assertEquals(twin.getGeoPosition().getLongitude(), state.getLastLongitude());
    assertEquals(twin.getHeadingDegrees(), state.getLastHeadingDegrees());
    assertEquals(twin.getCourseOverGroundDegrees(), state.getLastCourseOverGroundDegrees());
    assertEquals(twin.getGroundSpeedMetersPerSecond(), state.getLastGroundSpeedMetersPerSecond());
    assertEquals(129999, handler.getPgn());
  }

  private static DroneTwin activeTwin() {
    DroneTwin twin = new DroneTwin("drone");
    twin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
    twin.setGpsValid(true);
    twin.setNavigationUpdatedAt(Instant.parse("2026-09-20T07:00:00Z"));
    twin.setHeadingDegrees(10.0);
    twin.setCourseOverGroundDegrees(20.0);
    twin.setGroundSpeedMetersPerSecond(1.0);

    GeoPosition position = new GeoPosition();
    position.setLatitude(38.0);
    position.setLongitude(-9.0);
    twin.setGeoPosition(position);
    return twin;
  }

  private static final class TestHandler extends AbstractDronePgnHandler {
    TestHandler() {
      super(129999, null);
    }

    @Override
    public String getName() {
      return "test";
    }

    boolean active(DroneTwin twin) {
      return isActiveAndPositionValid(twin);
    }

    boolean material(DroneTwin twin, PgnEmissionState state) {
      return hasMaterialMotionChange(twin, state);
    }

    void snapshot(DroneTwin twin, PgnEmissionState state) {
      updateMotionSnapshot(twin, state);
    }

    double angular(Double previous, Double current) {
      return angularDifferenceDegrees(previous, current);
    }

    double absolute(Double previous, Double current) {
      return absoluteDifference(previous, current);
    }

    double distance(double lat1, double lon1, double lat2, double lon2) {
      return distanceMeters(lat1, lon1, lat2, lon2);
    }
  }
}
