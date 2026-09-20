package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VelocityVectorCoverageSweepTest {
  @Test
  void velocityComponentsCanBeConstructedAndUpdated() {
    VelocityVector velocity = new VelocityVector(5.0, -2.0, 1.0);

    assertEquals(5.0, velocity.getNorthMetersPerSecond());
    assertEquals(-2.0, velocity.getEastMetersPerSecond());
    assertEquals(1.0, velocity.getDownMetersPerSecond());

    velocity.setDownMetersPerSecond(-1.0);
    assertEquals(-1.0, velocity.getDownMetersPerSecond());
  }
}
