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

package io.mapsmessaging.config.network.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.KeyStoreConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.MemoryConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.ShmConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryConfigTest {

  @Test
  void listener_loads_unsecured_shm_config() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("url", "shm://localhost:0");
    properties.put("localAddress", "/run/user/1000/maps/mqtt.sock");
    EndPointServerConfig config = new EndPointServerConfig(properties);
    MemoryConfigDTO memory = assertInstanceOf(MemoryConfigDTO.class, config.getEndPointConfig());
    assertEquals("shm", memory.getType());
    assertEquals("/run/user/1000/maps/mqtt.sock", memory.getLocalAddress());
    assertFalse(memory.getMemorySecurity().isEnabled());
    assertEquals(64, memory.getMaxConnections());
  }

  @Test
  void outbound_and_listener_config_round_trip_security_and_limits() {
    ConfigurationProperties memory = new ConfigurationProperties();
    memory.put("maxConnections", 7);
    memory.put("handshakeTimeoutMillis", 1234);
    ConfigurationProperties security = new ConfigurationProperties();
    security.put("enabled", true);
    security.put("identity", "server-a");
    security.put("protection", "CHACHA20_POLY1305");
    ConfigurationProperties peers = new ConfigurationProperties();
    ConfigurationProperties currentAlias = new ConfigurationProperties();
    currentAlias.put("alias", "server-b-current");
    ConfigurationProperties nextAlias = new ConfigurationProperties();
    nextAlias.put("alias", "server-b-next");
    peers.put("server-b", List.of(currentAlias, nextAlias));
    security.put("trustedPeers", peers);
    ConfigurationProperties store = new ConfigurationProperties();
    store.put("type", "PKCS12");
    store.put("path", "identity.p12");
    store.put("passphrase", "store-password");
    store.put("alias", "server-a-key");
    security.put("keyStore", store);
    security.put("trustStore", store);
    security.put("keyPassword", "entry-password");
    memory.put("memorySecurity", security);
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "rdma-link");
    properties.put("url", "rdma://127.0.0.1:7471");
    properties.put("endPoint", memory);
    EndPointConnectionServerConfig original = new EndPointConnectionServerConfig(properties);
    EndPointConnectionServerConfig restored = new EndPointConnectionServerConfig(original.toConfigurationProperties());
    assertEquals(original.getEndPointConfig(), restored.getEndPointConfig());
    MemoryConfigDTO config = (MemoryConfigDTO) restored.getEndPointConfig();
    assertTrue(config.getMemorySecurity().isEnabled());
    assertEquals(7, config.getMaxConnections());
    assertEquals(1234, config.getHandshakeTimeoutMillis());
    assertEquals("server-a-key", config.getMemorySecurity().getKeyStore().getAlias());
    assertEquals(List.of("server-b-current", "server-b-next"), config.getMemorySecurity().getTrustedPeers().get("server-b"));
  }

  @Test
  void json_selects_shm_and_rdma_dtos() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    for (String type : List.of("shm", "rdma")) {
      EndPointConfigDTO config = mapper.readValue("{\"type\":\"" + type + "\",\"memorySecurity\":{\"enabled\":false}}", EndPointConfigDTO.class);
      assertInstanceOf(MemoryConfigDTO.class, config);
      assertEquals(type, config.getType());
    }
  }
  @Test
  void update_preserves_security_changes_and_exact_buffer_sizes() {
    ShmConfig config = new ShmConfig(new ConfigurationProperties());
    ShmConfigDTO update = new ShmConfigDTO();
    update.setServerReadBufferSize(12345);
    update.setServerWriteBufferSize(23456);
    update.setMaxNegotiations(3);
    update.getMemorySecurity().setEnabled(true);
    update.getMemorySecurity().setIdentity("server-a");
    update.getMemorySecurity().setKeyPassword(" entry$password ");
    assertTrue(config.update(update));
    assertFalse(config.update(update));
    ShmConfig restored = new ShmConfig(config.toConfigurationProperties());
    assertEquals(12345, restored.getServerReadBufferSize());
    assertEquals(23456, restored.getServerWriteBufferSize());
    assertEquals(" entry$password ", restored.getMemorySecurity().getKeyPassword());
    update.getMemorySecurity().setEnabled(false);
    assertTrue(config.update(update));
    assertFalse(config.getMemorySecurity().isEnabled());
  }

  @Test
  void malformed_security_flags_are_rejected() {
    ConfigurationProperties security = new ConfigurationProperties();
    security.put("enabled", "tru");
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("memorySecurity", security);
    assertThrows(IllegalArgumentException.class, () -> new ShmConfig(config));
  }

  @Test
  void updating_security_from_plain_dto_is_idempotent() {
    ShmConfig config = new ShmConfig(new ConfigurationProperties());
    ShmConfigDTO update = new ShmConfigDTO();
    KeyStoreConfigDTO store = new KeyStoreConfigDTO();
    store.setPath("identity.p12");
    store.setAlias("server-a");
    store.setPassphrase("password");
    update.getMemorySecurity().setKeyStore(store);
    update.getMemorySecurity().setTrustStore(store);
    assertTrue(config.update(update));
    assertFalse(config.update(update));
  }

}
