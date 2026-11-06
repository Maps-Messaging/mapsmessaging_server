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
import io.mapsmessaging.config.network.SslConfig;
import io.mapsmessaging.dto.rest.config.network.KeyStoreConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.TlsConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakServerDTO;
import io.mapsmessaging.network.EndPointURL;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakConnectionConfigConverterTest {

  @Test
  void absent_or_topic_only_configuration_has_no_remote_connections() {
    assertTrue(TakConnectionConfigConverter.convert(null).isEmpty());
    TakProtocolDTO tak = new TakProtocolDTO();
    tak.setTopic("/tak/cot");
    assertTrue(TakConnectionConfigConverter.convert(tak).isEmpty());
  }

  @Test
  void primary_tcp_connection_pushes_the_exact_topic_as_cot() {
    TakProtocolDTO tak = primary();

    List<EndPointConnectionServerConfig> entries = TakConnectionConfigConverter.convert(tak);

    assertEquals(1, entries.size());
    EndPointConnectionServerConfig entry = entries.getFirst();
    assertEquals("tcp://tak.example.org:8088", entry.getUrl());
    assertEquals("tcp", entry.getEndPointConfig().getType());
    assertNotNull(entry.getProtocolConfig("cot"));
    assertNotNull(entry.getAuthConfig());
    assertEquals("", entry.getGroupName());
    assertEquals(1, entry.getLinkConfigs().size());
    LinkConfigDTO link = entry.getLinkConfigs().getFirst();
    assertEquals("push", link.getDirection());
    assertEquals("/tak/cot", link.getLocalNamespace());
    assertEquals("/tak/cot", link.getRemoteNamespace());
    assertFalse(link.isIncludeSchema());
    assertEquals(0, link.getQualityOfService().getLevel());
  }

  @Test
  void additional_servers_follow_the_primary_with_independent_tls_settings() {
    TakProtocolDTO tak = primary();
    TakServerDTO second = server("second.example.org", 8089);
    second.setTlsEnabled(true);
    second.setTlsContext("TLSv1.3");
    second.setKeyStore(store("client.p12"));
    second.setTrustStore(store("trust.p12"));
    tak.setAdditionalServers(List.of(second, server("third.example.org", 8088)));

    List<EndPointConnectionServerConfig> entries = TakConnectionConfigConverter.convert(tak);

    assertEquals(List.of("tcp://tak.example.org:8088", "ssl://second.example.org:8089", "tcp://third.example.org:8088"), entries.stream().map(EndPointConnectionServerConfig::getUrl).toList());
    assertEquals(3, entries.stream().map(EndPointConnectionServerConfig::getName).distinct().count());
    SslConfig ssl = (SslConfig) ((TlsConfigDTO) entries.get(1).getEndPointConfig()).getSslConfig();
    assertEquals("TLSv1.3", ssl.getContext());
    assertEquals("client.p12", ssl.getKeyStore().getPath());
    assertEquals("trust.p12", ssl.getTrustStore().getPath());
    assertTrue(ssl.isHostnameVerificationEnabled());
    assertEquals("tcp", entries.get(2).getEndPointConfig().getType());
  }

  @Test
  void additional_servers_can_be_configured_without_a_primary_socket() {
    TakProtocolDTO tak = new TakProtocolDTO();
    tak.setTopic("/tak/out");
    tak.setAdditionalServers(List.of(server("additional.example.org", 8088)));

    List<EndPointConnectionServerConfig> entries = TakConnectionConfigConverter.convert(tak);

    assertEquals(1, entries.size());
    assertEquals("tcp://additional.example.org:8088", entries.getFirst().getUrl());
    assertEquals("/tak/out", entries.getFirst().getLinkConfigs().getFirst().getLocalNamespace());
  }

  @Test
  void tls_configuration_is_copied_and_can_round_trip_through_runtime_configuration() {
    TakProtocolDTO tak = primary();
    tak.setTlsEnabled(true);
    tak.setTlsContext("TLSv1.2");
    tak.setKeyStore(store("primary-client.p12"));
    tak.setTrustStore(store("primary-trust.p12"));
    tak.getKeyStore().setAlias("client-alias");
    tak.getKeyStore().setProviderName("provider-name");
    tak.getKeyStore().setProvider("provider");
    EndPointConnectionServerConfig entry = TakConnectionConfigConverter.convert(tak).getFirst();

    EndPointConnectionServerConfig restored = new EndPointConnectionServerConfig(entry.toConfigurationProperties());
    SslConfig ssl = (SslConfig) ((TlsConfigDTO) restored.getEndPointConfig()).getSslConfig();

    assertEquals("TLSv1.2", ssl.getContext());
    assertStoreEquals(tak.getKeyStore(), ssl.getKeyStore());
    assertStoreEquals(tak.getTrustStore(), ssl.getTrustStore());
    assertNotSame(tak.getKeyStore(), ssl.getKeyStore());
    assertNotNull(restored.getProtocolConfig("cot"));
    assertEquals("/tak/cot", restored.getLinkConfigs().getFirst().getLocalNamespace());
    ssl.getKeyStore().setPath("changed.p12");
    assertEquals("primary-client.p12", tak.getKeyStore().getPath());
  }

  @Test
  void conversion_is_repeatable_and_ignores_the_legacy_per_twin_socket_flag() {
    TakProtocolDTO tak = primary();
    tak.setAdditionalServers(null);
    List<EndPointConnectionServerConfig> first = TakConnectionConfigConverter.convert(tak);
    tak.setSharedConnection(true);
    List<EndPointConnectionServerConfig> second = TakConnectionConfigConverter.convert(tak);

    assertEquals(first.getFirst().toConfigurationProperties().getMap(), second.getFirst().toConfigurationProperties().getMap());
    assertNotSame(first.getFirst(), second.getFirst());
    assertEquals("tak.example.org", tak.getHostname());
  }

  @Test
  void ipv6_host_is_preserved_in_the_generated_endpoint_url() {
    TakProtocolDTO tak = primary();
    tak.setHostname("2001:db8::1");

    EndPointURL url = new EndPointURL(TakConnectionConfigConverter.convert(tak).getFirst().getUrl());

    assertEquals("2001:db8::1", url.getHost());
    assertEquals(8088, url.getPort());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 65536})
  void invalid_remote_ports_are_rejected(int port) {
    TakProtocolDTO tak = primary();
    tak.setPort(port);
    assertThrows(IllegalArgumentException.class, () -> TakConnectionConfigConverter.convert(tak));
  }

  @Test
  void remote_connections_require_an_outbound_topic() {
    TakProtocolDTO tak = primary();
    tak.setTopic(" ");
    assertThrows(IllegalArgumentException.class, () -> TakConnectionConfigConverter.convert(tak));
  }

  @Test
  void tls_connections_require_both_configured_stores() {
    TakProtocolDTO tak = primary();
    tak.setTlsEnabled(true);
    assertThrows(IllegalArgumentException.class, () -> TakConnectionConfigConverter.convert(tak));
    tak.setKeyStore(store("client.p12"));
    assertThrows(IllegalArgumentException.class, () -> TakConnectionConfigConverter.convert(tak));
  }

  @Test
  void malformed_additional_servers_are_rejected() {
    TakProtocolDTO tak = primary();
    tak.setAdditionalServers(List.of(new TakServerDTO()));
    assertThrows(IllegalArgumentException.class, () -> TakConnectionConfigConverter.convert(tak));
    List<TakServerDTO> servers = new ArrayList<>();
    servers.add(null);
    tak.setAdditionalServers(servers);
    assertThrows(IllegalArgumentException.class, () -> TakConnectionConfigConverter.convert(tak));
  }

  private static void assertStoreEquals(KeyStoreConfigDTO expected, KeyStoreConfigDTO actual) {
    assertEquals(expected.getAlias(), actual.getAlias());
    assertEquals(expected.getType(), actual.getType());
    assertEquals(expected.getProviderName(), actual.getProviderName());
    assertEquals(expected.getManagerFactory(), actual.getManagerFactory());
    assertEquals(expected.getPath(), actual.getPath());
    assertEquals(expected.getPassphrase(), actual.getPassphrase());
    assertEquals(expected.getProvider(), actual.getProvider());
  }

  private static TakProtocolDTO primary() {
    TakProtocolDTO tak = new TakProtocolDTO();
    tak.setHostname("tak.example.org");
    tak.setTopic("/tak/cot");
    return tak;
  }

  private static TakServerDTO server(String host, int port) {
    TakServerDTO server = new TakServerDTO();
    server.setHostname(host);
    server.setPort(port);
    return server;
  }

  private static KeyStoreConfigDTO store(String path) {
    KeyStoreConfigDTO store = new KeyStoreConfigDTO();
    store.setPath(path);
    store.setPassphrase("test-only");
    return store;
  }
}
