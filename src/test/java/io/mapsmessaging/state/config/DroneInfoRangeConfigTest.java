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

package io.mapsmessaging.state.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.mapsmessaging.configuration.ConfigurationProperties;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DroneInfoRangeConfigTest {

  @Test
  void constructor_readsOptionalRangeAndSurveyRadius() throws ReflectiveOperationException {
    TwinManagerConfig config = newTwinManagerConfig(rootWithDrone(25_000.0d, 200.0d));

    assertEquals(1, config.getDroneInfo().size());
    assertEquals(25_000.0d, config.getDroneInfo().get(0).getRangeMeters());
    assertEquals(200.0d, config.getDroneInfo().get(0).getSurveyRadiusMeters());
  }

  @Test
  void toConfigurationProperties_writesOptionalRangeAndSurveyRadius() throws ReflectiveOperationException {
    TwinManagerConfig config = newTwinManagerConfig(rootWithDrone(25_000.0d, 200.0d));

    ConfigurationProperties saved = config.toConfigurationProperties();
    List<?> droneInfos = assertInstanceOf(List.class, saved.get("droneInfo"));
    ConfigurationProperties drone = assertInstanceOf(ConfigurationProperties.class, droneInfos.get(0));

    assertEquals(25_000.0d, drone.getDoubleProperty("rangeMeters", 0.0d));
    assertEquals(200.0d, drone.getDoubleProperty("surveyRadiusMeters", 0.0d));
  }

  @Test
  void configurationRoundTrip_preservesOptionalRangeAndSurveyRadius() throws ReflectiveOperationException {
    TwinManagerConfig original = newTwinManagerConfig(rootWithDrone(25_000.0d, 200.0d));

    TwinManagerConfig reloaded = newTwinManagerConfig(original.toConfigurationProperties());

    assertEquals(25_000.0d, reloaded.getDroneInfo().get(0).getRangeMeters());
    assertEquals(200.0d, reloaded.getDroneInfo().get(0).getSurveyRadiusMeters());
  }

  @Test
  void constructor_whenRangeIsMissing_leavesRangeValidationDisabled() throws ReflectiveOperationException {
    ConfigurationProperties root = new ConfigurationProperties();
    ConfigurationProperties drone = baseDrone();
    root.put("droneInfo", List.of(drone));

    TwinManagerConfig config = newTwinManagerConfig(root);
    ConfigurationProperties saved = config.toConfigurationProperties();
    List<?> droneInfos = assertInstanceOf(List.class, saved.get("droneInfo"));
    ConfigurationProperties savedDrone = assertInstanceOf(ConfigurationProperties.class, droneInfos.get(0));

    assertNull(config.getDroneInfo().get(0).getRangeMeters());
    assertNull(savedDrone.get("rangeMeters"));
  }

  @Test
  void constructor_whenRangeIsNotPositive_ignoresRange() throws ReflectiveOperationException {
    TwinManagerConfig config = newTwinManagerConfig(rootWithDrone(0.0d, null));

    assertNull(config.getDroneInfo().get(0).getRangeMeters());
  }

  private ConfigurationProperties rootWithDrone(Double rangeMeters, Double surveyRadiusMeters) {
    ConfigurationProperties root = new ConfigurationProperties();
    ConfigurationProperties drone = baseDrone();
    if (rangeMeters != null) {
      drone.put("rangeMeters", rangeMeters);
    }
    if (surveyRadiusMeters != null) {
      drone.put("surveyRadiusMeters", surveyRadiusMeters);
    }
    root.put("droneInfo", List.of(drone));
    return root;
  }

  private ConfigurationProperties baseDrone() {
    ConfigurationProperties drone = new ConfigurationProperties();
    drone.put("name", "USV-001");
    drone.put("uuid", UUID.fromString("bd6bfbc4-22b8-5915-90d9-f9e23e7e63e6").toString());
    return drone;
  }

  private TwinManagerConfig newTwinManagerConfig(ConfigurationProperties properties) throws ReflectiveOperationException {
    Constructor<TwinManagerConfig> constructor = TwinManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }
}
