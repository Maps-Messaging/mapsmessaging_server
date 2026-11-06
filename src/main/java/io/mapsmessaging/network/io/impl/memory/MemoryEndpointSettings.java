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

package io.mapsmessaging.network.io.impl.memory;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.KeyStoreConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.MemoryConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.MemorySecurityConfigDTO;
import io.mapsmessaging.memory.MemoryConnectionConfig;
import io.mapsmessaging.memory.MemoryEndpointConfig;
import io.mapsmessaging.memory.MemoryTransportType;
import io.mapsmessaging.memory.WindowsPipeAddress;
import io.mapsmessaging.memory.security.MemorySecurity;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.security.certificates.CertificateManager;
import io.mapsmessaging.security.certificates.CertificateManagerFactory;
import io.mapsmessaging.security.certificates.TrustedPeerKeyResolver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/** Converts only memory endpoint configuration; keys and native handles stay in their owning libraries. */
final class MemoryEndpointSettings {
  private MemoryEndpointSettings() {
  }

  static MemoryEndpointConfig listener(EndPointURL url, MemoryConfigDTO config) throws IOException {
    try {
      validate(url, config);
      return MemoryEndpointConfig.builder().transport(transport(config)).address(address(url, config))
          .slotSize(config.getSlotSize()).slotCount(config.getSlotCount())
          .maxConnections(config.getMaxConnections()).maxPendingConnections(config.getMaxPendingConnections())
          .maxNegotiations(config.getMaxNegotiations()).handshakeTimeoutMillis(config.getHandshakeTimeoutMillis())
          .security(security(config)).build();
    } catch (GeneralSecurityException | IllegalArgumentException | NullPointerException exception) {
      throw new IOException("Invalid memory listener configuration", exception);
    }
  }

  static MemoryConnectionConfig client(EndPointURL url, MemoryConfigDTO config) throws IOException {
    try {
      validate(url, config);
      return new MemoryConnectionConfig(transport(config), address(url, config), Math.toIntExact(config.getConnectionTimeout()), security(config));
    } catch (GeneralSecurityException | IllegalArgumentException | ArithmeticException | NullPointerException exception) {
      throw new IOException("Invalid memory connection configuration", exception);
    }
  }

  private static void validate(EndPointURL url, MemoryConfigDTO config) {
    if (!("shm".equals(config.getType()) || "rdma".equals(config.getType())) || !config.getType().equals(url.getProtocol())) {
      throw new IllegalArgumentException("Memory transport type must match the URL scheme");
    }
    if (config.getReadinessPollMicros() < 1 || config.getReadinessPollMicros() > 1_000_000) {
      throw new IllegalArgumentException("readinessPollMicros must be between 1 and 1000000");
    }
    Objects.requireNonNull(config.getMemorySecurity(), "memorySecurity");
    if (config.getMemorySecurity().isEnabled() && "shm".equals(config.getType()) && !config.isNativeSharedMemory()) {
      throw new IllegalArgumentException("Secured shared memory requires a native local address");
    }
  }

  private static MemoryTransportType transport(MemoryConfigDTO config) {
    return "rdma".equals(config.getType()) ? MemoryTransportType.RDMA : MemoryTransportType.SHARED_MEMORY;
  }

  private static SocketAddress address(EndPointURL url, MemoryConfigDTO config) {
    if ("shm".equals(config.getType()) && config.isNativeSharedMemory()) {
      String local = config.getLocalAddress();
      if (local == null || local.isBlank()) {
        throw new IllegalArgumentException("Native shared memory requires localAddress");
      }
      return System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows")
          ? WindowsPipeAddress.of(local) : UnixDomainSocketAddress.of(local);
    }
    return new InetSocketAddress(url.getHost(), url.getPort());
  }

  private static MemorySecurity security(MemoryConfigDTO endpoint) throws IOException, GeneralSecurityException {
    MemorySecurityConfigDTO config = endpoint.getMemorySecurity();
    if (!config.isEnabled()) {
      return null;
    }
    KeyStoreConfigDTO store = Objects.requireNonNull(config.getKeyStore(), "keyStore");
    String alias = store.getAlias();
    if (alias == null || alias.isBlank() || config.getIdentity() == null || config.getIdentity().isBlank()) {
      throw new IllegalArgumentException("Secured memory transport requires explicit identity and key alias");
    }
    if (config.getTrustedPeers() == null || config.getTrustedPeers().isEmpty()) {
      throw new IllegalArgumentException("Secured memory transport requires explicit trusted peers");
    }
    CertificateManager keys = manager(store);
    CertificateManager trusted = manager(Objects.requireNonNull(config.getTrustStore(), "trustStore"));
    String password = config.getKeyPassword() == null ? store.getPassphrase() : config.getKeyPassword();
    char[] entryPassword = password == null ? new char[0] : password.toCharArray();
    PrivateKey signingKey;
    PublicKey verificationKey;
    try {
      synchronized (keys) {
        signingKey = keys.getKey(alias, entryPassword);
        verificationKey = keys.getCertificate(alias).getPublicKey();
      }
    } finally {
      Arrays.fill(entryPassword, '\0');
    }
    TrustedPeerKeyResolver resolver = new TrustedPeerKeyResolver(trusted, config.getTrustedPeers());
    for (String peer : config.getTrustedPeers().keySet()) {
      resolver.getTrustedKeys(peer);
    }
    return MemorySecurity.builder().identity(config.getIdentity(), signingKey, verificationKey)
        .protection(config.getProtection()).trust((peer, key) -> {
          try {
            return resolver.isTrusted(peer, key);
          } catch (CertificateException exception) {
            throw new IllegalStateException("Memory peer certificate could not be verified", exception);
          }
        }).build();
  }

  private static CertificateManager manager(KeyStoreConfigDTO store) throws IOException, GeneralSecurityException {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("type", store.getType());
    properties.put("path", store.getPath());
    properties.put("passphrase", store.getPassphrase());
    properties.put("providerName", store.getProviderName());
    properties.put("provider", store.getProvider());
    CertificateManager manager = CertificateManagerFactory.getInstance().getManager(properties);
    if (!manager.getExists()) {
      throw new IOException("Configured memory security keystore does not exist");
    }
    return manager;
  }
}
