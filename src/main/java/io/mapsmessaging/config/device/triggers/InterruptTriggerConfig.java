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

package io.mapsmessaging.config.device.triggers;


import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class InterruptTriggerConfig extends TriggerConfig {
  private int address;
  private String pullDirection;
  private String id;

  public InterruptTriggerConfig(ConfigurationProperties config) {
    super(config);
    address = config.getIntProperty("address", 0);
    pullDirection = config.getProperty("pull", "UP");
    id = config.getProperty("id", "");
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = super.toConfigurationProperties();
    config.put("address", address);
    config.put("pull", pullDirection);
    config.put("id", id);
    return config;
  }

}
