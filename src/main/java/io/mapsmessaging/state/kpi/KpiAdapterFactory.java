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

package io.mapsmessaging.state.kpi;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.state.adapter.StateMessageAdapter;
import io.mapsmessaging.state.adapter.StateMessageAdapterContext;
import io.mapsmessaging.state.adapter.StateMessageAdapterFactory;

import java.util.Map;
import java.util.Optional;

/**
 * SPI entry point, discovered by {@code StateManagerAgent.loadStateMessageAdapters()}. Only starts
 * if {@code stateAdapters.kpi} is present in {@code TwinManager.yaml} - i.e. on the node that holds
 * the full picture, not on every edge. See {@link KpiConfig} for the keys.
 */
public class KpiAdapterFactory implements StateMessageAdapterFactory {

  private static final String ADAPTER_KEY = "kpi";

  @Override
  public String getName() {
    return "kpi";
  }

  @Override
  public Optional<StateMessageAdapter> create(StateMessageAdapterContext context) {
    Map<String, ConfigurationProperties> adapterConfig = context.getConfig().getAdapterConfig();
    ConfigurationProperties props = adapterConfig == null ? null : adapterConfig.get(ADAPTER_KEY);
    if (props == null) {
      return Optional.empty();
    }
    return Optional.of(new KpiAdapter(KpiConfig.from(props), context.getTwinManager()));
  }
}
