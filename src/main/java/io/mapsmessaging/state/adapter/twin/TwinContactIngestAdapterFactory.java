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

package io.mapsmessaging.state.adapter.twin;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.state.adapter.StateMessageAdapter;
import io.mapsmessaging.state.adapter.StateMessageAdapterContext;
import io.mapsmessaging.state.adapter.StateMessageAdapterFactory;
import java.util.Map;
import java.util.Optional;

/**
 * Enables {@link TwinContactIngestAdapter} when the TwinManager configuration asks for it:
 *
 * <pre>
 *   stateAdapters:
 *     twinContactIngest:
 *       topic: "/state/twins/+/contacts"   # the default
 * </pre>
 *
 * Intended for the node that holds the TAK connection: edge nodes relay their twins and the
 * contacts they detect, and this turns those contacts back into detections so they can be drawn
 * on the picture the aggregator publishes. Absent the key the adapter is not created at all.
 */
public class TwinContactIngestAdapterFactory implements StateMessageAdapterFactory {

  private static final String ADAPTER_KEY = "twinContactIngest";
  static final String DEFAULT_TOPIC = "/state/twins/+/contacts";

  @Override
  public String getName() {
    return "twin-contact-ingest";
  }

  @Override
  public Optional<StateMessageAdapter> create(StateMessageAdapterContext context) {
    Map<String, ConfigurationProperties> adapterConfig = context.getConfig().getAdapterConfig();
    ConfigurationProperties props = adapterConfig == null ? null : adapterConfig.get(ADAPTER_KEY);
    if (props == null) {
      return Optional.empty();
    }
    String topic = props.getProperty("topic", DEFAULT_TOPIC);
    return Optional.of(new TwinContactIngestAdapter(topic, context.getTwinManager()));
  }
}
