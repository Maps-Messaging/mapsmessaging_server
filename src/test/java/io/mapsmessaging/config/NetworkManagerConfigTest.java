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

package io.mapsmessaging.config;

import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.config.network.impl.TlsConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NetworkManagerConfigTest {

  @Test
  void endpoint_update_changes_the_stored_tls_configuration() {
    EndPointServerConfig storedConfig = new EndPointServerConfig(createEndpoint(false));
    EndPointServerConfig update = new EndPointServerConfig(createEndpoint(true));
    NetworkManagerConfig networkManagerConfig = new NetworkManagerConfig();
    networkManagerConfig.setEndPointServerConfigList(new ArrayList<>(List.of(storedConfig)));

    Assertions.assertTrue(networkManagerConfig.update(update));

    TlsConfig storedTlsConfig = (TlsConfig) storedConfig.getEndPointConfig();
    Assertions.assertTrue(storedTlsConfig.getSslConfig().isClientCertificateRequired());
  }

  private ConfigurationProperties createEndpoint(boolean clientCertificateRequired) {
    ConfigurationProperties keyStore = createStore("test-keystore.jks");
    ConfigurationProperties trustStore = createStore("test-truststore.jks");

    ConfigurationProperties tls = new ConfigurationProperties();
    tls.put("context", "TLSv1.3");
    tls.put("clientCertificateRequired", clientCertificateRequired);
    tls.put("clientCertificateWanted", false);
    tls.put("hostnameVerificationEnabled", true);
    tls.put("keyStore", keyStore);
    tls.put("trustStore", trustStore);

    ConfigurationProperties security = new ConfigurationProperties();
    security.put("tls", tls);

    ConfigurationProperties config = new ConfigurationProperties();
    config.put("name", "TLS endpoint");
    config.put("url", "ssl://localhost:8883");
    config.put("auth", "default");
    config.put("protocol", "loop");
    config.put("security", security);
    return config;
  }

  private ConfigurationProperties createStore(String path) {
    ConfigurationProperties store = new ConfigurationProperties();
    store.put("type", "JKS");
    store.put("managerFactory", "SunX509");
    store.put("path", path);
    store.put("passphrase", "password");
    return store;
  }
}
