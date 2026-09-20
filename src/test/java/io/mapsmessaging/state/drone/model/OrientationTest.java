package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrientationTest {

  @Test
  void yawIsNormalisedIntoCompassHeadingRange() {
    assertEquals(10.0, new Orientation(0.0, 0.0, 370.0).getHeadingFromYaw(), 0.0);
    assertEquals(350.0, new Orientation(0.0, 0.0, -10.0).getHeadingFromYaw(), 0.0);
    assertEquals(0.0, new Orientation(0.0, 0.0, 720.0).getHeadingFromYaw(), 0.0);
  }

  @Test
  void nullYawProducesNoHeading() {
    assertNull(new Orientation(1.0, 2.0, null).getHeadingFromYaw());
  }

  @Test
  void alreadyNormalisedYawIsPreserved() {
    assertEquals(182.5, new Orientation(null, null, 182.5).getHeadingFromYaw(), 0.0);
  }
}
