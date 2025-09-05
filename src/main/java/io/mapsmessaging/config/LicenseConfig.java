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

package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Getter
@NoArgsConstructor
public class LicenseConfig implements ConfigManager {

  private String clientName;
  private String clientSecret;

  public static LicenseConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(LicenseConfig.class);
  }

  private LicenseConfig(ConfigurationProperties properties) {
    clientName = properties.getProperty("name", "");
    clientSecret = properties.getProperty("secret", "");
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new LicenseConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    // there is no option to save here
  }

  @Override
  public String getName() {
    return "License";
  }
}
