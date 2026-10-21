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

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.dto.rest.config.protocol.impl.TakServerDTO;

import javax.net.ssl.SSLSocketFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Sends every CoT event the observer produces to the configured additional TAK servers, so they
 * receive exactly the same (MTI-augmented) XML as the primary server. Each server has its own
 * connection, queue and writer thread: an unreachable server only fills and drops its own queue
 * and never delays the others.
 */
final class AdditionalTakServers {

  private final List<TakSocketConnection> connections;

  AdditionalTakServers(List<TakServerDTO> servers, Function<TakServerDTO, SSLSocketFactory> sslSocketFactories) {
    List<TakSocketConnection> created = new ArrayList<>();
    if (servers != null) {
      for (TakServerDTO server : servers) {
        created.add(new TakSocketConnection(server.getHostname(), server.getPort(), sslSocketFactories.apply(server)));
      }
    }
    this.connections = Collections.unmodifiableList(created);
  }

  static AdditionalTakServers none() {
    return new AdditionalTakServers(List.of(), server -> null);
  }

  boolean isEmpty() {
    return connections.isEmpty();
  }

  List<TakSocketConnection> getConnections() {
    return connections;
  }

  void accept(String xml) {
    for (TakSocketConnection connection : connections) {
      connection.accept(xml);
    }
  }

  void close() {
    for (TakSocketConnection connection : connections) {
      connection.close();
    }
  }
}
