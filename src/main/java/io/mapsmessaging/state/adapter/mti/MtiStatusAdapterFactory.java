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

package io.mapsmessaging.state.adapter.mti;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.state.adapter.StateMessageAdapter;
import io.mapsmessaging.state.adapter.StateMessageAdapterContext;
import io.mapsmessaging.state.adapter.StateMessageAdapterFactory;

import java.util.Map;
import java.util.Optional;

/**
 * SPI entry point ({@code ServiceLoader}, see {@code META-INF/services}) - discovered and invoked
 * by {@code StateManagerAgent.loadStateMessageAdapters()} at server startup, same mechanism the
 * (retired) MILCO connector and OSI-4559-MAPS-Connector use. Only starts if
 * {@code stateAdapters.mti} is present in {@code TwinManager.yaml}, matching the MILCO adapter's
 * own config convention:
 *
 * <pre>
 * stateAdapters:
 *   mti:
 *     topic: "/mti/status/feed"   # the MTI team's spec default is /tak/cot - override if that
 *                                 # collides with this deployment's own TwinManager.yaml tak.topic
 * </pre>
 */
public class MtiStatusAdapterFactory implements StateMessageAdapterFactory {

  private static final String ADAPTER_KEY = "mti";
  private static final String DEFAULT_TOPIC = "/tak/cot"; // the MTI team's own spec default

  @Override
  public String getName() {
    return "mti-status";
  }

  @Override
  public Optional<StateMessageAdapter> create(StateMessageAdapterContext context) {
    Map<String, ConfigurationProperties> adapterConfig = context.getConfig().getAdapterConfig();
    ConfigurationProperties props = adapterConfig == null ? null : adapterConfig.get(ADAPTER_KEY);
    if (props == null) {
      return Optional.empty();
    }
    String topic = props.getProperty("topic", DEFAULT_TOPIC);
    return Optional.of(new MtiStatusAdapter(topic));
  }
}
