/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.config.network;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class HmacConfig extends Config {

  private String host;
  private int port;
  private String secret;
  private String hmacAlgorithm;
  private String hmacManager;
  private String hmacSharedKey;


  public HmacConfig(ConfigurationProperties config){
    host = config.getProperty("host");
    port = config.getIntProperty("port", 0);
    hmacAlgorithm = config.getProperty("HmacAlgorithm");
    if (hmacAlgorithm != null) {
      hmacManager = config.getProperty("HmacManager", "Appender");
      hmacSharedKey = config.getProperty("HmacSharedKey");
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    return null;
  }
}
