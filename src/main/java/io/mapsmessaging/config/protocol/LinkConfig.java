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

package io.mapsmessaging.config.protocol;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class LinkConfig extends Config {

  private String direction;
  private String remoteNamespace;
  private String localNamespace;
  private String selector;
  private Map<String, Object> transformer;

  public LinkConfig(ConfigurationProperties config) {
    direction = config.getProperty("direction");
    remoteNamespace = config.getProperty("remote_namespace");
    localNamespace = config.getProperty("local_namespace");
    selector = config.getProperty("selector");
    Object obj = config.get("transformer");
    if (obj instanceof ConfigurationProperties) {
      transformer = ((ConfigurationProperties) obj).getMap();
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("direction", direction);
    config.put("remote_namespace", remoteNamespace);
    config.put("local_namespace", remoteNamespace);
    config.put("selector", selector);
    config.put("transformer", new ConfigurationProperties(transformer));
    return config;
  }

  public boolean update(LinkConfig newConfig) {
    boolean hasChanged = false;

    if (this.direction == null || !this.direction.equals(newConfig.getDirection())) {
      this.direction = newConfig.getDirection();
      hasChanged = true;
    }

    if (this.remoteNamespace == null || !this.remoteNamespace.equals(newConfig.getRemoteNamespace())) {
      this.remoteNamespace = newConfig.getRemoteNamespace();
      hasChanged = true;
    }

    if (this.remoteNamespace == null || !this.remoteNamespace.equals(newConfig.getLocalNamespace())) {
      this.remoteNamespace = newConfig.getLocalNamespace();
      hasChanged = true;
    }
    if (this.selector == null || !this.selector.equals(newConfig.getSelector())) {
      this.selector = newConfig.getSelector();
      hasChanged = true;
    }
    if(super.updateMap(transformer, newConfig.getTransformer())) {
      hasChanged = true;
    }
    return hasChanged;
  }


}