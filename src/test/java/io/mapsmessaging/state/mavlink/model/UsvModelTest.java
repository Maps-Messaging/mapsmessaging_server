package io.mapsmessaging.state.mavlink.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UsvModelTest {

  @Test
  void unsupportedSurfaceVehicleOperationsIdentifyExactOperation() {
    UsvModel model = model();

    assertUnsupported(model, UxvOperation.ORBIT, () -> model.orbit(null, null));
    assertUnsupported(model, UxvOperation.LOITER, () -> model.loiter(null, null));
    assertUnsupported(model, UxvOperation.SET_SPEED, () -> model.setSpeed(null, 3));
    assertUnsupported(model, UxvOperation.SET_HEADING, () -> model.setHeading(null, 45));
    assertUnsupported(model, UxvOperation.SET_TURN_RATE, () -> model.setTurnRate(null, 2));
  }

  private static void assertUnsupported(
      UsvModel model,
      UxvOperation operation,
      Executable executable
  ) {
    UnsupportedUxvOperationException failure =
        assertThrows(UnsupportedUxvOperationException.class, executable);
    assertEquals("test-usv", failure.getModelName());
    assertSame(operation, failure.getOperation());
  }

  private static UsvModel model() {
    return new UsvModel() {
      @Override
      public String getModelName() {
        return "test-usv";
      }

      @Override
      public UxvVehicleType getVehicleType() {
        return UxvVehicleType.USV;
      }

      @Override
      public Set<UxvOperation> getSupportedOperations() {
        return Set.of();
      }
    };
  }
}
