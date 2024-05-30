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

package io.mapsmessaging.network;

import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Getter;

@Getter
public class NetworkConfig {

  protected static final String PROPERTY_URL = "url";
  private static final String PROPERTY_NAME = "auth";
  private static final String PROPERTY_PROTOCOL = "protocol";

  private final String url;
  private final String authConfig;
  private final String protocols;

  private final ConfigurationProperties properties;

  public NetworkConfig(ConfigurationProperties properties) {
    url = properties.getProperty(PROPERTY_URL);

    String auth = properties.getProperty(PROPERTY_NAME);
    if (auth == null) {
      auth = "";
    }
    authConfig = auth;

    String prot = properties.getProperty(PROPERTY_PROTOCOL);
    if (prot == null) {
      prot = "all";
    }
    protocols = prot.toLowerCase();

    this.properties = properties;
  }

}
