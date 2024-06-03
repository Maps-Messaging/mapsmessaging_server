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

import io.mapsmessaging.config.auth.AuthConfig;
import io.mapsmessaging.config.protocol.LinkConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class EndPointConnectionServerConfig extends EndPointServerConfig{

  private AuthConfig authConfig;
  private List<LinkConfig> linkConfigs;

  public EndPointConnectionServerConfig(ConfigurationProperties props){
    super(props);
    authConfig = new AuthConfig(props);
    linkConfigs = new ArrayList<>();
    Object obj = props.get("links");
    if (obj instanceof List) {
      for (ConfigurationProperties entry : (List<ConfigurationProperties>) obj) {
        linkConfigs.add(new LinkConfig(entry));
      }
    } else if (obj instanceof ConfigurationProperties) {
      linkConfigs.add(new LinkConfig((ConfigurationProperties) obj));
    }
  }

  public boolean update(EndPointConnectionServerConfig newConfig) {
    return super.update(newConfig) || this.authConfig.update(newConfig.getAuthConfig());
  }
}
