/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.drone.tak.model.TakEvent;

public class CotEventClassifier {

  public CotObjectClass classify(TakEvent event, boolean configuredManagedPlatform) {
    if (event == null || event.getType() == null) {
      return CotObjectClass.PROTOCOL_CONTROL;
    }
    String type = event.getType().toLowerCase();
    if (type.startsWith("t-") || type.startsWith("y-") || type.startsWith("b-t-f")) {
      return CotObjectClass.PROTOCOL_CONTROL;
    }
    if (type.startsWith("u-d-") || type.contains("detection")) {
      return CotObjectClass.DETECTION;
    }
    if (type.startsWith("u-") || type.startsWith("b-m-") || type.startsWith("b-r-")) {
      return CotObjectClass.MAP_ARTEFACT;
    }
    if (configuredManagedPlatform) {
      return CotObjectClass.MANAGED_PLATFORM;
    }
    return CotObjectClass.OBSERVED_PLATFORM;
  }
}
