/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at:
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
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MavlinkSourceRegistryTest {

  @Test
  void null_or_empty_configuration_matches_no_sources() {
    MavlinkTwinConfigDTO nullConfig = new MavlinkTwinConfigDTO();
    nullConfig.setKnownSources(null);
    assertNull(new MavlinkSourceRegistry(nullConfig).getKnownSource(frame(1, 1)));

    MavlinkTwinConfigDTO emptyConfig = new MavlinkTwinConfigDTO();
    assertNull(new MavlinkSourceRegistry(emptyConfig).getKnownSource(frame(1, 1)));
  }

  @Test
  void exact_system_and_component_pair_matches_configured_source() {
    MavlinkKnownSourceDTO knownSource = source("drone-1", 17, 42);
    MavlinkTwinConfigDTO config = config(knownSource);

    MavlinkKnownSourceDTO result = new MavlinkSourceRegistry(config).getKnownSource(frame(17, 42));

    assertSame(knownSource, result);
  }

  @Test
  void different_system_or_component_is_not_related() {
    MavlinkKnownSourceDTO knownSource = source("drone-1", 17, 42);
    MavlinkSourceRegistry registry = new MavlinkSourceRegistry(config(knownSource));

    assertNull(registry.getKnownSource(frame(18, 42)));
    assertNull(registry.getKnownSource(frame(17, 43)));
  }

  @Test
  void duplicate_source_key_uses_last_configuration_entry() {
    MavlinkKnownSourceDTO first = source("first", 17, 42);
    MavlinkKnownSourceDTO second = source("second", 17, 42);

    MavlinkKnownSourceDTO result = new MavlinkSourceRegistry(config(first, second)).getKnownSource(frame(17, 42));

    assertSame(second, result);
  }

  private MavlinkTwinConfigDTO config(MavlinkKnownSourceDTO... sources) {
    MavlinkTwinConfigDTO config = new MavlinkTwinConfigDTO();
    config.setKnownSources(List.of(sources));
    return config;
  }

  private MavlinkKnownSourceDTO source(String name, int systemId, int componentId) {
    MavlinkKnownSourceDTO source = new MavlinkKnownSourceDTO();
    source.setName(name);
    source.setSystemId(systemId);
    source.setComponentId(componentId);
    return source;
  }

  private ProcessedFrame frame(int systemId, int componentId) {
    ProcessedFrame processedFrame = mock(ProcessedFrame.class, RETURNS_DEEP_STUBS);
    when(processedFrame.getFrame().getSystemId()).thenReturn(systemId);
    when(processedFrame.getFrame().getComponentId()).thenReturn(componentId);
    return processedFrame;
  }
}
