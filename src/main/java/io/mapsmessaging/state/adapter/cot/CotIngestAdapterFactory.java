/*
 *
 *  Copyright [ 2026 ] Ralf Himmelein and Claude
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

package io.mapsmessaging.state.adapter.cot;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.state.adapter.StateMessageAdapter;
import io.mapsmessaging.state.adapter.StateMessageAdapterContext;
import io.mapsmessaging.state.adapter.StateMessageAdapterFactory;

import java.util.Map;
import java.util.Optional;

/**
 * SPI entry point ({@code ServiceLoader}, see {@code META-INF/services}) - discovered and invoked
 * by {@code StateManagerAgent.loadStateMessageAdapters()} at server startup, same mechanism
 * {@code MtiStatusAdapterFactory} uses. Only starts if {@code stateAdapters.cotIngest} is present
 * in {@code TwinManager.yaml}:
 *
 * <pre>
 * stateAdapters:
 *   cotIngest:
 *     topic: "/tak/cot/inbound/#"   # wildcard: covers every edge without per-edge config here
 * </pre>
 *
 * Intended for a MAPS-to-MAPS deployment topology: one or more edge nodes each run their own
 * ingest (mavlink/n2k/CoT) + baseline CoT composition, then replicate their own
 * {@code TwinManagerConfig.tak.topic} onto a distinct leaf under this wildcard via their own
 * {@code NetworkConnectionManager.yaml} outbound bridge. This node (typically a central
 * aggregator with the authoritative MTI feed and the real TAK-server link) never needs
 * reconfiguring as edges are added or removed.
 */
public class CotIngestAdapterFactory implements StateMessageAdapterFactory {

  private static final String ADAPTER_KEY = "cotIngest";
  private static final String DEFAULT_TOPIC = "/tak/cot/inbound/#";

  @Override
  public String getName() {
    return "cot-ingest";
  }

  @Override
  public Optional<StateMessageAdapter> create(StateMessageAdapterContext context) {
    Map<String, ConfigurationProperties> adapterConfig = context.getConfig().getAdapterConfig();
    ConfigurationProperties props = adapterConfig == null ? null : adapterConfig.get(ADAPTER_KEY);
    if (props == null) {
      return Optional.empty();
    }
    String topic = props.getProperty("topic", DEFAULT_TOPIC);
    return Optional.of(new CotIngestAdapter(topic, context.getTwinManager()));
  }
}
