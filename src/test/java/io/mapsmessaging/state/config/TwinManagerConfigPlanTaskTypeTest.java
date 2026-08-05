/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 */

package io.mapsmessaging.state.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.state.config.capability.PlanTaskType;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;

class TwinManagerConfigPlanTaskTypeTest {

  @Test
  void loadsBarePlanTaskType() throws Exception {
    assertLoadedTaskType("SCREEN", PlanTaskType.SCREEN);
  }

  @Test
  void loadsPlanTaskTypePrefix() throws Exception {
    assertLoadedTaskType("PlanTaskType_SCREEN", PlanTaskType.SCREEN);
  }

  @Test
  void loadsPlanTaskTypeEnumPrefix() throws Exception {
    assertLoadedTaskType("PlanTaskTypeEnum_SCREEN", PlanTaskType.SCREEN);
  }

  private void assertLoadedTaskType(String configuredValue, PlanTaskType expected)
      throws Exception {
    TwinManagerConfig config = loadConfig(configuredValue);

    PlanTaskType actual =
        config.getDroneInfo()
            .get(0)
            .getCapabilities()
            .getTasks()
            .get(0)
            .getTaskType();

    assertEquals(expected, actual);
  }

  private TwinManagerConfig loadConfig(String taskType) throws Exception {
    ConfigurationProperties taskCapability = new ConfigurationProperties();
    taskCapability.put("task_type", taskType);

    ConfigurationProperties capabilities = new ConfigurationProperties();
    capabilities.put("tasks", List.of(taskCapability));

    ConfigurationProperties drone = new ConfigurationProperties();
    drone.put("name", "USV-001");
    drone.put("capabilities", capabilities);

    ConfigurationProperties root = new ConfigurationProperties();
    root.put("droneInfo", List.of(drone));

    Constructor<TwinManagerConfig> constructor =
        TwinManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(root);
  }
}
