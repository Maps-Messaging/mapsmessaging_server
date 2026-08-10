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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import org.junit.jupiter.api.Test;

class DroneSurveyRadiusConfigurationTest {

  @Test
  void surveyRadiusDeserializesAndCanBePropagatedToTwin() {
    DroneInfoDTO configured =
        new Gson().fromJson("{\"surveyRadiusMeters\":200.0}", DroneInfoDTO.class);

    DroneTwin twin = new DroneTwin("survey-drone");
    twin.setSurveyRadiusMeters(configured.getSurveyRadiusMeters());

    assertEquals(200.0d, configured.getSurveyRadiusMeters());
    assertEquals(200.0d, twin.getSurveyRadiusMeters());
    assertEquals(400.0d, twin.getSurveyRadiusMeters() * 2.0d);
  }

  @Test
  void surveyRadiusRemainsOptional() {
    DroneInfoDTO configured = new Gson().fromJson("{}", DroneInfoDTO.class);
    DroneTwin twin = new DroneTwin("non-survey-drone");
    twin.setSurveyRadiusMeters(configured.getSurveyRadiusMeters());

    assertNull(configured.getSurveyRadiusMeters());
    assertNull(twin.getSurveyRadiusMeters());
  }
}
