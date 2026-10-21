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

package io.mapsmessaging.state.mavlink.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4FixedWingUavModel;
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4UavModel;
import io.mapsmessaging.state.mavlink.model.impl.ugv.GenericPx4UgvModel;
import io.mapsmessaging.state.mavlink.model.impl.usv.SticklebackArdupilotUsvModel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class UxvModelServiceLoaderTest {

  @Test
  void serviceLoaderDiscoversEveryConfiguredModelWithUniqueNames() {
    Map<String, UxvModel> models = new LinkedHashMap<>();

    for (UxvModel model : ServiceLoader.load(UxvModel.class)) {
      UxvModel previous = models.putIfAbsent(model.getModelName(), model);
      assertNull(
          previous,
          "Duplicate UxV model name " + model.getModelName());
    }

    assertModel(
        models,
        GenericPx4UavModel.MODEL_NAME,
        GenericPx4UavModel.class,
        UxvVehicleType.UAV);
    assertModel(
        models,
        GenericPx4FixedWingUavModel.MODEL_NAME,
        GenericPx4FixedWingUavModel.class,
        UxvVehicleType.UAV);
    assertModel(
        models,
        GenericPx4UgvModel.MODEL_NAME,
        GenericPx4UgvModel.class,
        UxvVehicleType.UGV);
    assertModel(
        models,
        SticklebackArdupilotUsvModel.MODEL_NAME,
        SticklebackArdupilotUsvModel.class,
        UxvVehicleType.USV);
  }

  private static void assertModel(
      Map<String, UxvModel> models,
      String modelName,
      Class<? extends UxvModel> expectedClass,
      UxvVehicleType expectedVehicleType) {
    UxvModel model = models.get(modelName);

    assertNotNull(model, "Model not registered: " + modelName);
    assertInstanceOf(expectedClass, model);
    assertEquals(expectedVehicleType, model.getVehicleType());
  }
}
