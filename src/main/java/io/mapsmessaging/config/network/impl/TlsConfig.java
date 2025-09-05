/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
import io.mapsmessaging.config.network.SslConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.TcpConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.TlsConfigDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class TlsConfig extends TlsConfigDTO implements Config {

  public TlsConfig(ConfigurationProperties config) {
    setType("tls");
    NetworkConfigFactory.unpack(config, this);
    sslConfig = new SslConfig(config);
    if(sslConfig.getContext() == null || sslConfig.getContext().isEmpty()) {
      sslConfig.setContext("TLSv1.3");
    }
  }

  public boolean update(BaseConfigDTO update) {
    boolean hasChanged = false;
    if (update instanceof TcpConfigDTO) {
      hasChanged = NetworkConfigFactory.update(this, (TcpConfigDTO) update);
      if(((SslConfig)sslConfig).update(update)){
        hasChanged = true;
      }
    }

    return hasChanged;
  }


  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    NetworkConfigFactory.pack(config, this);
    ConfigurationProperties security = new ConfigurationProperties();
    security.put("tls", ((SslConfig)sslConfig).toConfigurationProperties());
    config.put("security", security);
    return config;
  }
}
