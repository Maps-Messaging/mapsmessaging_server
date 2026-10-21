package io.mapsmessaging.state.mavlink.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UgvModelTest {

  @Test
  void unsupportedGroundVehicleOperationsIdentifyExactOperation() {
    UgvModel model = model();

    assertUnsupported(model, UxvOperation.ORBIT, () -> model.orbit(null, null));
    assertUnsupported(model, UxvOperation.LOITER, () -> model.loiter(null, null));
    assertUnsupported(model, UxvOperation.SET_SPEED, () -> model.setSpeed(null, 3));
    assertUnsupported(model, UxvOperation.SET_HEADING, () -> model.setHeading(null, 45));
    assertUnsupported(model, UxvOperation.SET_TURN_RATE, () -> model.setTurnRate(null, 2));
  }

  private static void assertUnsupported(
      UgvModel model,
      UxvOperation operation,
      Executable executable
  ) {
    UnsupportedUxvOperationException failure =
        assertThrows(UnsupportedUxvOperationException.class, executable);
    assertEquals("test-ugv", failure.getModelName());
    assertSame(operation, failure.getOperation());
  }

  private static UgvModel model() {
    return new UgvModel() {
      @Override
      public String getModelName() {
        return "test-ugv";
      }

      @Override
      public UxvVehicleType getVehicleType() {
        return UxvVehicleType.UGV;
      }

      @Override
      public Set<UxvOperation> getSupportedOperations() {
        return Set.of();
      }
    };
  }
}
