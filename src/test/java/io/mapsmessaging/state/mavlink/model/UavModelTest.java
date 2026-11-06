package io.mapsmessaging.state.mavlink.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UavModelTest {

  @Test
  void unsupportedUavSpecificOperationsIdentifyExactOperation() {
    UavModel model = model();

    assertUnsupported(model, UxvOperation.TAKE_OFF, () -> model.takeOff(null, 10));
    assertUnsupported(model, UxvOperation.LAND, () -> model.land(null));
    assertUnsupported(model, UxvOperation.SET_ALTITUDE, () -> model.setAltitude(null, 20));
    assertUnsupported(model, UxvOperation.ORBIT, () -> model.orbit(null, null));
    assertUnsupported(model, UxvOperation.LOITER, () -> model.loiter(null, null));
    assertUnsupported(model, UxvOperation.SET_SPEED, () -> model.setSpeed(null, 5));
    assertUnsupported(model, UxvOperation.SET_HEADING, () -> model.setHeading(null, 90));
  }

  private static void assertUnsupported(
      UavModel model,
      UxvOperation operation,
      Executable executable
  ) {
    UnsupportedUxvOperationException failure =
        assertThrows(UnsupportedUxvOperationException.class, executable);
    assertEquals("test-uav", failure.getModelName());
    assertSame(operation, failure.getOperation());
  }

  private static UavModel model() {
    return new UavModel() {
      @Override
      public String getModelName() {
        return "test-uav";
      }

      @Override
      public UxvVehicleType getVehicleType() {
        return UxvVehicleType.UAV;
      }

      @Override
      public Set<UxvOperation> getSupportedOperations() {
        return Set.of();
      }
    };
  }
}
