package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.Orientation;
import io.mapsmessaging.state.drone.model.VelocityVector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class TakEventMapperFinalCoverageTest {

  @Test
  void identifierSanitizingCollapsesWhitespacePunctuationAndEmptyValues() throws Exception {
    TakEventMapper mapper = new TakEventMapper();
    Method method = TakEventMapper.class.getDeclaredMethod("sanitiseIdentifier", String.class);
    method.setAccessible(true);

    assertEquals("unknown", method.invoke(mapper, new Object[]{null}));
    assertEquals("unknown", method.invoke(mapper, "   "));
    assertEquals("alpha-beta", method.invoke(mapper, "  alpha   beta  "));
    assertEquals("a-b.c_d", method.invoke(mapper, "--a@@b.c_d--"));
  }

  @Test
  void degreeNormalizationWrapsNegativeAndLargeAngles() throws Exception {
    TakEventMapper mapper = new TakEventMapper();
    Method method = TakEventMapper.class.getDeclaredMethod("normaliseDegrees", double.class);
    method.setAccessible(true);

    assertEquals(0.0, (double) method.invoke(mapper, 0.0), 0.0);
    assertEquals(350.0, (double) method.invoke(mapper, -10.0), 0.0);
    assertEquals(5.0, (double) method.invoke(mapper, 725.0), 0.0);
    assertEquals(180.0, (double) method.invoke(mapper, 540.0), 0.0);
  }

  @Test
  void flightModeRejectsNullBlankAndNumericModesButKeepsNamedModes() throws Exception {
    TakEventMapper mapper = new TakEventMapper();
    Method method = TakEventMapper.class.getDeclaredMethod("resolveFlightMode", DroneTwin.class);
    method.setAccessible(true);

    assertNull(method.invoke(mapper, new Object[]{null}));

    DroneTwin twin = new DroneTwin("d");
    twin.setFlightMode(null);
    assertNull(method.invoke(mapper, twin));
    twin.setFlightMode(" ");
    assertNull(method.invoke(mapper, twin));
    twin.setFlightMode("123");
    assertNull(method.invoke(mapper, twin));
    twin.setFlightMode("AUTO");
    assertEquals("AUTO", method.invoke(mapper, twin));
  }

  @Test
  void courseResolutionUsesCogThenVelocityThenHeadingThenYawThenZero() throws Exception {
    TakEventMapper mapper = new TakEventMapper();
    Method method = TakEventMapper.class.getDeclaredMethod(
        "resolveCourseDegrees",
        io.mapsmessaging.state.drone.core.EntityTwin.class,
        Orientation.class,
        VelocityVector.class);
    method.setAccessible(true);

    DroneTwin twin = new DroneTwin("d");
    VelocityVector velocity = new VelocityVector(0.0, 1.0, 0.0);
    Orientation orientation = new Orientation(0.0, 0.0, -5.0);

    twin.setCourseOverGroundDegrees(-10.0);
    assertEquals(350.0, (double) method.invoke(mapper, twin, orientation, velocity), 0.0);

    twin.setCourseOverGroundDegrees(null);
    assertEquals(90.0, (double) method.invoke(mapper, twin, orientation, velocity), 0.0001);

    velocity.setNorthMetersPerSecond(0.0);
    velocity.setEastMetersPerSecond(0.0);
    twin.setHeadingDegrees(725.0);
    assertEquals(5.0, (double) method.invoke(mapper, twin, orientation, velocity), 0.0);

    twin.setHeadingDegrees(null);
    assertEquals(355.0, (double) method.invoke(mapper, twin, orientation, velocity), 0.0);

    orientation.setYawDegrees(null);
    assertEquals(0.0, (double) method.invoke(mapper, twin, orientation, velocity), 0.0);
  }
}