/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.test;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class ExternalDependencyExecutionCondition implements ExecutionCondition {

  static final String CANBUS_FLAG = "maps.tests.canbus";
  static final String SATELLITE_FLAG = "maps.tests.satellite";

  private static final String N2K_CANBUS_TEST =
      "io.mapsmessaging.network.protocol.impl.n2k.N2kViaCanBusTest";
  private static final String CANAEROSPACE_CANBUS_TEST =
      "io.mapsmessaging.network.protocol.impl.canaerospace.CanAerospaceViaCanBusTest";
  private static final String SATELLITE_FORWARD_TEST =
      "io.mapsmessaging.network.protocol.impl.satellite.SatelliteReplicationTests";
  private static final String SATELLITE_REVERSE_TEST =
      "io.mapsmessaging.network.protocol.impl.satellite.SatelliteReplicationReverseTests";
  private static final String SCHEMA_ROUND_TRIP_TEST =
      "io.mapsmessaging.tools.config.schema.tests.JsonSchemaRoundTripTest";

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Class<?> testClass = context.getTestClass().orElse(null);
    if (testClass == null) {
      return ConditionEvaluationResult.enabled("No test class context");
    }

    String className = testClass.getName();

    if (SCHEMA_ROUND_TRIP_TEST.equals(className)) {
      return ConditionEvaluationResult.disabled(
          "MSG-308: runtime JSON schema/Jackson round-trip contract is under review");
    }

    if (isCanBusTest(className) && !Boolean.getBoolean(CANBUS_FLAG)) {
      return ConditionEvaluationResult.disabled(
          "Requires virtual CAN interfaces; enable with -D" + CANBUS_FLAG + "=true");
    }

    if (isSatelliteDaemonTest(className) && !Boolean.getBoolean(SATELLITE_FLAG)) {
      return ConditionEvaluationResult.disabled(
          "Requires satellite daemon configuration; enable with -D" + SATELLITE_FLAG + "=true");
    }

    return ConditionEvaluationResult.enabled("External dependency requirements satisfied");
  }

  private static boolean isCanBusTest(String className) {
    return N2K_CANBUS_TEST.equals(className) || CANAEROSPACE_CANBUS_TEST.equals(className);
  }

  private static boolean isSatelliteDaemonTest(String className) {
    return SATELLITE_FORWARD_TEST.equals(className) || SATELLITE_REVERSE_TEST.equals(className);
  }
}
