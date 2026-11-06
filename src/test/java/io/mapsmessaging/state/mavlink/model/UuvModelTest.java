package io.mapsmessaging.state.mavlink.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UuvModelTest {

  @Test
  void unsupportedUnderwaterOperationsIdentifyExactOperation() {
    UuvModel model = model();

    assertUnsupported(model, UxvOperation.DIVE, () -> model.dive(null, 10));
    assertUnsupported(model, UxvOperation.SURFACE, () -> model.surface(null));
    assertUnsupported(model, UxvOperation.SET_DEPTH, () -> model.setDepth(null, 20));
    assertUnsupported(model, UxvOperation.HOLD_DEPTH, () -> model.holdDepth(null, 20));
    assertUnsupported(model, UxvOperation.ORBIT, () -> model.orbit(null, null));
    assertUnsupported(model, UxvOperation.LOITER, () -> model.loiter(null, null));
    assertUnsupported(model, UxvOperation.SET_SPEED, () -> model.setSpeed(null, 2));
    assertUnsupported(model, UxvOperation.SET_HEADING, () -> model.setHeading(null, 180));
  }

  private static void assertUnsupported(
      UuvModel model,
      UxvOperation operation,
      Executable executable
  ) {
    UnsupportedUxvOperationException failure =
        assertThrows(UnsupportedUxvOperationException.class, executable);
    assertEquals("test-uuv", failure.getModelName());
    assertSame(operation, failure.getOperation());
  }

  private static UuvModel model() {
    return new UuvModel() {
      @Override
      public String getModelName() {
        return "test-uuv";
      }

      @Override
      public UxvVehicleType getVehicleType() {
        return UxvVehicleType.UUV;
      }

      @Override
      public Set<UxvOperation> getSupportedOperations() {
        return Set.of();
      }
    };
  }
}
