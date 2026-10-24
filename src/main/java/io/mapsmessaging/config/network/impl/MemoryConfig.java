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
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.MemoryConfigDTO;

import java.util.Objects;

public abstract class MemoryConfig extends MemoryConfigDTO implements Config {
  protected MemoryConfig(String type, ConfigurationProperties properties) {
    super(type);
    ConfigurationProperties endpoint = new ConfigurationProperties(properties.getMap());
    endpoint.setGlobal(properties.getGlobal());
    if (properties.get("endPoint") instanceof ConfigurationProperties nested) {
      endpoint.putAll(nested.getMap());
    }
    NetworkConfigFactory.unpack(endpoint, this);
    localAddress = endpoint.getProperty("localAddress");
    nativeSharedMemory = endpoint.getBooleanProperty("nativeSharedMemory", true);
    slotSize = endpoint.getIntProperty("slotSize", slotSize);
    slotCount = endpoint.getIntProperty("slotCount", slotCount);
    maxConnections = endpoint.getIntProperty("maxConnections", maxConnections);
    maxPendingConnections = endpoint.getIntProperty("maxPendingConnections", maxPendingConnections);
    maxNegotiations = endpoint.getIntProperty("maxNegotiations", maxNegotiations);
    handshakeTimeoutMillis = endpoint.getIntProperty("handshakeTimeoutMillis", handshakeTimeoutMillis);
    readinessPollMicros = endpoint.getLongProperty("readinessPollMicros", readinessPollMicros);
    Object security = endpoint.get("memorySecurity");
    if (security == null && endpoint.get("security") instanceof ConfigurationProperties securityProperties) {
      security = securityProperties.get("memory");
    }
    memorySecurity = new MemorySecurityConfig(security == null ? new ConfigurationProperties() : (ConfigurationProperties) security);
  }

  @Override
  public boolean update(BaseConfigDTO update) {
    if (!(update instanceof MemoryConfigDTO config) || !type.equals(config.getType())) {
      return false;
    }
    MemorySecurityConfig security = new MemorySecurityConfig(Objects.requireNonNull(config.getMemorySecurity(), "memorySecurity"));
    boolean changed = NetworkConfigFactory.update(this, config)
        || !Objects.equals(getProxyProtocolMode(), config.getProxyProtocolMode())
        || !Objects.equals(getAllowedProxyHosts(), config.getAllowedProxyHosts())
        || !Objects.equals(localAddress, config.getLocalAddress())
        || nativeSharedMemory != config.isNativeSharedMemory()
        || slotSize != config.getSlotSize() || slotCount != config.getSlotCount()
        || maxConnections != config.getMaxConnections() || maxPendingConnections != config.getMaxPendingConnections()
        || maxNegotiations != config.getMaxNegotiations() || handshakeTimeoutMillis != config.getHandshakeTimeoutMillis()
        || readinessPollMicros != config.getReadinessPollMicros()
        || !new MemorySecurityConfig(memorySecurity).equals(security);
    if (changed) {
      setProxyProtocolMode(config.getProxyProtocolMode());
      setAllowedProxyHosts(config.getAllowedProxyHosts());
      localAddress = config.getLocalAddress();
      nativeSharedMemory = config.isNativeSharedMemory();
      slotSize = config.getSlotSize();
      slotCount = config.getSlotCount();
      maxConnections = config.getMaxConnections();
      maxPendingConnections = config.getMaxPendingConnections();
      maxNegotiations = config.getMaxNegotiations();
      handshakeTimeoutMillis = config.getHandshakeTimeoutMillis();
      readinessPollMicros = config.getReadinessPollMicros();
      memorySecurity = security;
    }
    return changed;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    NetworkConfigFactory.pack(properties, this);
    properties.put("connectionTimeout", getConnectionTimeout());
    // Preserve exact byte counts; the generic formatter rounds non-integral KiB values.
    properties.put("serverReadBufferSize", getServerReadBufferSize());
    properties.put("serverWriteBufferSize", getServerWriteBufferSize());
    properties.put("localAddress", localAddress);
    properties.put("nativeSharedMemory", nativeSharedMemory);
    properties.put("slotSize", slotSize);
    properties.put("slotCount", slotCount);
    properties.put("maxConnections", maxConnections);
    properties.put("maxPendingConnections", maxPendingConnections);
    properties.put("maxNegotiations", maxNegotiations);
    properties.put("handshakeTimeoutMillis", handshakeTimeoutMillis);
    properties.put("readinessPollMicros", readinessPollMicros);
    properties.put("memorySecurity", new MemorySecurityConfig(memorySecurity).toConfigurationProperties());
    return properties;
  }
}
