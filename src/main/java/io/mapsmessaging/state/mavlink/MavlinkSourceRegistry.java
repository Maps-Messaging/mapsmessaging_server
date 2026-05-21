/*
 *
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

package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.VehicleClass;
import io.mapsmessaging.dto.rest.config.twin.MavlinkTwinConfigDTO;
import io.mapsmessaging.mavlink.ProcessedFrame;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class MavlinkSourceRegistry {

  private final Map<String, MavlinkKnownSourceDTO> knownSources;

  public MavlinkSourceRegistry(@NonNull @NotNull MavlinkTwinConfigDTO mavlinkConfig) {
    this.knownSources = buildKnownSources(mavlinkConfig);
  }

  public MavlinkKnownSourceDTO getKnownSource(ProcessedFrame env) {
    String sourceKey = buildSourceKey(env.getFrame().getSystemId(), env.getFrame().getComponentId());

    return knownSources.get(sourceKey);
  }

  private Map<String, MavlinkKnownSourceDTO> buildKnownSources(MavlinkTwinConfigDTO mavlinkConfig) {
    Map<String, MavlinkKnownSourceDTO> sources = new HashMap<>();
    List<MavlinkKnownSourceDTO> knownSourceList = mavlinkConfig.getKnownSources();

    if (knownSourceList == null) {
      return sources;
    }

    for (MavlinkKnownSourceDTO knownSource : knownSourceList) {
      String sourceKey = buildSourceKey(knownSource.getSystemId(), knownSource.getComponentId());
      sources.put(sourceKey, knownSource);
    }

    return sources;
  }

  private String buildSourceKey(int systemId, int componentId) {
    return systemId + ":" + componentId;
  }
}