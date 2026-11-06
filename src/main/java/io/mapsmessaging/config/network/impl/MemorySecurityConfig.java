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

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.network.KeyStoreConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.KeyStoreConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.MemorySecurityConfigDTO;
import io.mapsmessaging.memory.security.DataProtection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MemorySecurityConfig extends MemorySecurityConfigDTO implements Config {
  public MemorySecurityConfig(ConfigurationProperties properties) {
    String configuredEnabled = properties.getProperty("enabled", "false");
    if (!"true".equalsIgnoreCase(configuredEnabled) && !"false".equalsIgnoreCase(configuredEnabled)) {
      throw new IllegalArgumentException("Memory security enabled must be true or false");
    }
    enabled = Boolean.parseBoolean(configuredEnabled);
    identity = properties.getProperty("identity");
    keyPassword = properties.getProperty("keyPassword");
    protection = DataProtection.valueOf(properties.getProperty("protection", "AES_256_GCM"));
    keyStore = readStore(properties, "keyStore");
    trustStore = readStore(properties, "trustStore");
    Object peers = properties.get("trustedPeers");
    if (peers != null) {
      ConfigurationProperties peerProperties = peers instanceof ConfigurationProperties configured ? configured : new ConfigurationProperties((Map<String, Object>) peers);
      for (String peer : peerProperties.keySet()) {
        Object aliases = peerProperties.get(peer);
        if (!(aliases instanceof List<?> list)) {
          throw new IllegalArgumentException("Trusted peers require a list of alias entries");
        }
        List<String> names = new ArrayList<>();
        for (Object entry : list) {
          if (!(entry instanceof ConfigurationProperties alias) || alias.getProperty("alias") == null) {
            throw new IllegalArgumentException("Trusted peer entries require alias");
          }
          names.add(alias.getProperty("alias"));
        }
        trustedPeers.put(peer, List.copyOf(names));
      }
    }
  }

  public MemorySecurityConfig(MemorySecurityConfigDTO source) {
    enabled = source.isEnabled();
    identity = source.getIdentity();
    keyPassword = source.getKeyPassword();
    protection = source.getProtection();
    keyStore = copyStore(source.getKeyStore());
    trustStore = copyStore(source.getTrustStore());
    trustedPeers = new LinkedHashMap<>();
    source.getTrustedPeers().forEach((peer, aliases) -> trustedPeers.put(peer, List.copyOf(aliases)));
  }

  @Override
  public boolean update(BaseConfigDTO update) {
    if (!(update instanceof MemorySecurityConfigDTO source)) {
      return false;
    }
    MemorySecurityConfig copy = new MemorySecurityConfig(source);
    if (equals(copy)) {
      return false;
    }
    enabled = copy.enabled;
    identity = copy.identity;
    keyPassword = copy.keyPassword;
    protection = copy.protection;
    keyStore = copy.keyStore;
    trustStore = copy.trustStore;
    trustedPeers = copy.trustedPeers;
    return true;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", enabled);
    properties.put("identity", identity);
    properties.put("keyPassword", keyPassword);
    properties.put("protection", protection.name());
    if (keyStore != null) {
      properties.put("keyStore", ((KeyStoreConfig) copyStore(keyStore)).toConfigurationProperties());
    }
    if (trustStore != null) {
      properties.put("trustStore", ((KeyStoreConfig) copyStore(trustStore)).toConfigurationProperties());
    }
    Map<String, Object> peers = new LinkedHashMap<>();
    trustedPeers.forEach((peer, aliases) -> {
      List<ConfigurationProperties> entries = new ArrayList<>();
      for (String alias : aliases) {
        ConfigurationProperties entry = new ConfigurationProperties();
        entry.put("alias", alias);
        entries.add(entry);
      }
      peers.put(peer, entries);
    });
    properties.put("trustedPeers", new ConfigurationProperties(peers));
    return properties;
  }

  private static KeyStoreConfigDTO readStore(ConfigurationProperties properties, String name) {
    Object store = properties.get(name);
    return store == null ? null : new KeyStoreConfig((ConfigurationProperties) store);
  }

  private static KeyStoreConfigDTO copyStore(KeyStoreConfigDTO source) {
    if (source == null) {
      return null;
    }
    KeyStoreConfig copy = new KeyStoreConfig(new ConfigurationProperties());
    copy.update(source);
    return copy;
  }
}
