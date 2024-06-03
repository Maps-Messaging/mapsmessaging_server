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

package io.mapsmessaging.config.auth;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class AuthConfig  extends Config {

  private String username;
  private String password;
  private String sessionId;
  private String tokenGenerator;
  private Map<String, Object> tokenConfig;

  public AuthConfig(ConfigurationProperties config) {
    ConfigurationProperties remoteAuth = (ConfigurationProperties) config.get("remote");
    tokenConfig = new LinkedHashMap<>();

    if(remoteAuth != null) {
      username = remoteAuth.getProperty("username");
      password = remoteAuth.getProperty("password");
      tokenGenerator = remoteAuth.getProperty("tokenGenerator");
      sessionId = remoteAuth.getProperty("sessionId");
      if(tokenGenerator != null && !tokenGenerator.isEmpty()){
        ConfigurationProperties tokenConfig = (ConfigurationProperties) remoteAuth.get("tokenConfig");
        tokenConfig.putAll(tokenConfig.getMap());
      }
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    ConfigurationProperties remoteAuth = new ConfigurationProperties();
    config.put("remote", remoteAuth);
    if(username != null) remoteAuth.put("username", username);
    if(password != null) remoteAuth.put("password", password);
    if(tokenGenerator != null) remoteAuth.put("tokenGenerator", tokenGenerator);
    if(sessionId != null) remoteAuth.put("sessionId", sessionId);
    if (tokenConfig != null) remoteAuth.put("tokenConfig", new ConfigurationProperties(tokenConfig));
    return config;
  }

  public boolean update(AuthConfig newConfig) {
    boolean hasChanged = false;

    if (this.username == null || !this.username.equals(newConfig.getUsername())) {
      this.username = newConfig.getUsername();
      hasChanged = true;
    }

    if (this.password == null || !this.password.equals(newConfig.getPassword())) {
      this.password = newConfig.getPassword();
      hasChanged = true;
    }

    if (this.tokenGenerator == null || !this.tokenGenerator.equals(newConfig.getTokenGenerator())) {
      this.tokenGenerator = newConfig.getTokenGenerator();
      hasChanged = true;
    }

    if (this.sessionId == null || !this.sessionId.equals(newConfig.getSessionId())) {
      this.sessionId = newConfig.getSessionId();
      hasChanged = true;
    }
    if (updateMap(this.tokenConfig, newConfig.getTokenConfig())) {
      hasChanged = true;
    }
    return hasChanged;
  }


}