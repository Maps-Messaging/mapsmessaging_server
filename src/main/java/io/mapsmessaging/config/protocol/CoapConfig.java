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

import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class CoapConfig extends ProtocolConfig {
  private int maxBlockSize;
  private int idleTime;

  public CoapConfig(ConfigurationProperties config) {
    super(config);
    maxBlockSize = config.getIntProperty("maxBlockSize", 128);
    idleTime = config.getIntProperty("idleTimePeriod", 120);
  }

  @Override
  public String getType() {
    return "coap";
  }

  public ConfigurationProperties toConfigurationProperties() {
    return super.toConfigurationProperties();
  }
}
