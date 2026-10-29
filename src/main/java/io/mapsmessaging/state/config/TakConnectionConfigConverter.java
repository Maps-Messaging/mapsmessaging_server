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

import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.config.network.KeyStoreConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.KeyStoreConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakServerDTO;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/** Converts legacy TAK remote-server settings into independent NetworkConnectionManager CoT push entries. */
public final class TakConnectionConfigConverter {

  private TakConnectionConfigConverter() {
  }

  /** Returns configuration entries without changing the source configuration, registering connections or opening sockets. */
  public static List<EndPointConnectionServerConfig> convert(TakProtocolDTO takConfig) {
    if (takConfig == null) {
      return List.of();
    }
    List<EndPointConnectionServerConfig> entries = new ArrayList<>();
    String hostname = takConfig.getHostname();
    if (hostname != null && !hostname.isBlank()) {
      entries.add(
        createEntry("state-tak-primary", hostname, takConfig.getPort(), takConfig.isTlsEnabled(), takConfig.getTlsContext(), takConfig.getKeyStore(), takConfig.getTrustStore(), takConfig.getTopic())
      );
    }
    if (takConfig.getAdditionalServers() != null) {
      int index = 1;
      for (TakServerDTO server : takConfig.getAdditionalServers()) {
        if (server == null) {
          throw new IllegalArgumentException("Additional TAK server configuration is required");
        }
        entries.add(
          createEntry("state-tak-additional-" + index++, server.getHostname(), server.getPort(), server.isTlsEnabled(), server.getTlsContext(), server.getKeyStore(), server.getTrustStore(), takConfig.getTopic())
        );
      }
    }
    return entries;
  }

  private static EndPointConnectionServerConfig createEntry(String name, String hostname, int port, boolean tlsEnabled, String tlsContext, KeyStoreConfigDTO keyStore, KeyStoreConfigDTO trustStore, String topic) {
    if (hostname == null || hostname.isBlank()) {
      throw new IllegalArgumentException("TAK server hostname is required");
    }
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("TAK server port must be between 1 and 65535");
    }
    if (topic == null || topic.isBlank()) {
      throw new IllegalArgumentException("TAK topic is required for remote server connections");
    }
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", name);
    properties.put("url", createUrl(tlsEnabled ? "ssl" : "tcp", hostname.trim(), port));
    properties.put("protocol", "cot");

    ConfigurationProperties link = new ConfigurationProperties();
    link.put("direction", "push");
    link.put("local_namespace", topic);
    // CoT has no on-wire namespace; the link still requires a remote binding value.
    link.put("remote_namespace", topic);
    link.put("include_schema", false);
    properties.put("links", List.of(link));

    if (tlsEnabled) {
      if (keyStore == null || trustStore == null) {
        throw new IllegalArgumentException("TAK TLS connections require a key store and trust store");
      }
      ConfigurationProperties tls = new ConfigurationProperties();
      tls.put("context", tlsContext);
      tls.put("keyStore", copyStore(keyStore));
      tls.put("trustStore", copyStore(trustStore));
      ConfigurationProperties security = new ConfigurationProperties();
      security.put("tls", tls);
      properties.put("security", security);
    }
    return new EndPointConnectionServerConfig(properties);
  }

  private static String createUrl(String scheme, String hostname, int port) {
    try {
      return new URI(scheme, null, hostname, port, null, null, null).toString();
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException("Invalid TAK server hostname", exception);
    }
  }

  private static ConfigurationProperties copyStore(KeyStoreConfigDTO source) {
    KeyStoreConfig copy = new KeyStoreConfig(new ConfigurationProperties());
    copy.update(source);
    return copy.toConfigurationProperties();
  }
}
