package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CotEventPolicyFinalCoverageTest {

  @Test
  void droneClassificationRequiresSingleAirBattleDimensionSegment() throws Exception {
    CotEventPolicy policy = new CotEventPolicy();
    Method method = CotEventPolicy.class.getDeclaredMethod("isDroneClassification", String.class);
    method.setAccessible(true);

    assertEquals(false, method.invoke(policy, new Object[]{null}));
    assertEquals(false, method.invoke(policy, "a-f"));
    assertEquals(false, method.invoke(policy, "a-f-AA"));
    assertEquals(true, method.invoke(policy, "a-f-A-M-F-U"));
    assertEquals(false, method.invoke(policy, "a-f-S"));
  }

  @Test
  void configuredErrorsMustBeFiniteAndNonNegativeOtherwiseDefaultIsUsed() throws Exception {
    CotEventPolicy policy = new CotEventPolicy();
    Method method = CotEventPolicy.class.getDeclaredMethod(
        "resolveError", Double.class, double.class);
    method.setAccessible(true);

    assertEquals(10.0, (double) method.invoke(policy, null, 10.0), 0.0);
    assertEquals(10.0, (double) method.invoke(policy, -1.0, 10.0), 0.0);
    assertEquals(10.0, (double) method.invoke(policy, Double.NaN, 10.0), 0.0);
    assertEquals(0.0, (double) method.invoke(policy, 0.0, 10.0), 0.0);
    assertEquals(4.5, (double) method.invoke(policy, 4.5, 10.0), 0.0);
  }

  @Test
  void uidPrefixingLeavesUidUntouchedForBlankPrefixAndHandlesNullUid() throws Exception {
    CotEventPolicy policy = new CotEventPolicy();
    Method method = CotEventPolicy.class.getDeclaredMethod(
        "prefixUid", String.class, String.class);
    method.setAccessible(true);

    assertEquals("id", method.invoke(policy, "id", null));
    assertEquals("id", method.invoke(policy, "id", " "));
    assertEquals("prefix-id", method.invoke(policy, "id", "prefix-"));
    assertEquals("prefix-", method.invoke(policy, null, "prefix-"));
  }

  @Test
  void sourceAffiliationUsesDescriptionWhenPresentAndVehicleClassFallbackOtherwise() throws Exception {
    CotEventPolicy policy = new CotEventPolicy();
    Method method = CotEventPolicy.class.getDeclaredMethod(
        "resolveSourceAffiliation",
        io.mapsmessaging.state.drone.core.EntityTwin.class);
    method.setAccessible(true);

    DroneTwin knownClass = new DroneTwin("known");
    knownClass.setVehicleClass(VehicleClass.UAV);
    assertEquals("f", method.invoke(policy, knownClass));

    DroneTwin unknownClass = new DroneTwin("unknown");
    unknownClass.setVehicleClass(VehicleClass.UNKNOWN);
    assertEquals("u", method.invoke(policy, unknownClass));

    DroneTwin hostile = new DroneTwin("hostile");
    hostile.setDescription(Map.of("standard_identity", "StandardIdentityEnum_HOSTILE"));
    assertEquals("h", method.invoke(policy, hostile));

    DroneTwin missing = new DroneTwin("missing");
    missing.setDescription(Map.of("other", "value"));
    assertEquals("u", method.invoke(policy, missing));
  }
}