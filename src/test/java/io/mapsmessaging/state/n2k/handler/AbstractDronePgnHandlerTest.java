package io.mapsmessaging.state.n2k.handler;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.n2k.DroneEmissionState;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractDronePgnHandlerTest {

  @Test
  void angularDifferenceUsesShortestPathAcrossNorth() {
    assertEquals(4.0d, TestHandler.angularDifference(358.0d, 2.0d), 0.0d);
    assertEquals(4.0d, TestHandler.angularDifference(2.0d, 358.0d), 0.0d);
    assertEquals(2.0d, TestHandler.angularDifference(-1.0d, 361.0d), 0.0d);
  }

  @Test
  void nullToNullValuesAreUnchangedButNullTransitionsAreMaterial() {
    assertEquals(0.0d, TestHandler.linearDifference(null, null));
    assertEquals(0.0d, TestHandler.angularDifference(null, null));
    assertEquals(Double.MAX_VALUE, TestHandler.linearDifference(null, 1.0d));
    assertEquals(Double.MAX_VALUE, TestHandler.angularDifference(1.0d, null));
  }

  @Test
  void identicalCoordinatesHaveZeroDistance() {
    assertEquals(0.0d, TestHandler.distance(-33.8688d, 151.2093d, -33.8688d, 151.2093d), 0.000001d);
  }

  @Test
  void oneDegreeAtEquatorIsAboutOneHundredElevenKilometres() {
    assertEquals(111_194.9d, TestHandler.distance(0.0d, 0.0d, 0.0d, 1.0d), 1.0d);
  }

  @Test
  void antimeridianCrossingUsesShortLongitudePath() {
    assertEquals(222.4d, TestHandler.distance(0.0d, 179.999d, 0.0d, -179.999d), 1.0d);
  }

  @Test
  void nearAntipodalCoordinatesProduceFiniteDistance() {
    double distance = TestHandler.distance(
        77.1499348704312d,
        -162.219605230478d,
        -77.1499348710738d,
        17.780394770358d);

    assertTrue(Double.isFinite(distance));
    assertEquals(20_015_086.8d, distance, 2.0d);
  }

  private static final class TestHandler extends AbstractDronePgnHandler {

    private TestHandler() {
      super(0, null);
    }

    private static double linearDifference(Double previous, Double current) {
      return absoluteDifference(previous, current);
    }

    private static double angularDifference(Double previous, Double current) {
      return angularDifferenceDegrees(previous, current);
    }

    private static double distance(double latitude1, double longitude1, double latitude2, double longitude2) {
      return distanceMeters(latitude1, longitude1, latitude2, longitude2);
    }

    @Override
    public String getName() {
      return "test";
    }

    @Override
    public Optional<PgnEmission> emit(DroneTwin droneTwin, DroneEmissionState droneEmissionState, long now) {
      return Optional.empty();
    }
  }
}
