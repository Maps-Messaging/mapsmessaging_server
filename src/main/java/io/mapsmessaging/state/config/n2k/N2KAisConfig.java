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

package io.mapsmessaging.state.config.n2k;

import io.mapsmessaging.configuration.ConfigurationProperties;

public class N2KAisConfig extends N2KAisConfigDTO {

  public N2KAisConfig(ConfigurationProperties config) {
    if (config.containsKey("pgn129039")) {
      ConfigurationProperties pgn129039Properties = (ConfigurationProperties) config.get("pgn129039");
      this.pgn129039 = new N2KPgnTransmitConfig(pgn129039Properties);
    }

    if (config.containsKey("pgn129040")) {
      ConfigurationProperties pgn129040Properties = (ConfigurationProperties) config.get("pgn129040");
      this.pgn129040 = new N2KPgnTransmitConfig(pgn129040Properties);
    }

    if (config.containsKey("pgn129809")) {
      ConfigurationProperties pgn129809Properties = (ConfigurationProperties) config.get("pgn129809");
      this.pgn129809 = new N2KPgnTransmitConfig(pgn129809Properties);
    }

    if (config.containsKey("pgn129810")) {
      ConfigurationProperties pgn129810Properties = (ConfigurationProperties) config.get("pgn129810");
      this.pgn129810 = new N2KPgnTransmitConfig(pgn129810Properties);
    }
  }

  public boolean update(N2KAisConfigDTO newConfig) {
    boolean hasChanged = false;
    if (pgn129039 == null && newConfig.getPgn129039() != null) {
      pgn129039 = newConfig.getPgn129039();
      hasChanged = true;
    } else if (pgn129039 != null && !pgn129039.equals(newConfig.getPgn129039())) {
      pgn129039 = newConfig.getPgn129039();
      hasChanged = true;
    }

    if (pgn129040 == null && newConfig.getPgn129040() != null) {
      pgn129040 = newConfig.getPgn129040();
      hasChanged = true;
    } else if (pgn129040 != null && !pgn129040.equals(newConfig.getPgn129040())) {
      pgn129040 = newConfig.getPgn129040();
      hasChanged = true;
    }

    if (pgn129809 == null && newConfig.getPgn129809() != null) {
      pgn129809 = newConfig.getPgn129809();
      hasChanged = true;
    } else if (pgn129809 != null && !pgn129809.equals(newConfig.getPgn129809())) {
      pgn129809 = newConfig.getPgn129809();
      hasChanged = true;
    }

    if (pgn129810 == null && newConfig.getPgn129810() != null) {
      pgn129810 = newConfig.getPgn129810();
      hasChanged = true;
    } else if (pgn129810 != null && !pgn129810.equals(newConfig.getPgn129810())) {
      pgn129810 = newConfig.getPgn129810();
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    return toConfigurationProperties(this);
  }

  public static ConfigurationProperties toConfigurationProperties(N2KAisConfigDTO config) {
    ConfigurationProperties properties = new ConfigurationProperties();

    if (config.getPgn129039() != null) {
      properties.put("pgn129039", toConfigurationProperties(config.getPgn129039()));
    }

    if (config.getPgn129040() != null) {
      properties.put("pgn129040", toConfigurationProperties(config.getPgn129040()));
    }

    if (config.getPgn129809() != null) {
      properties.put("pgn129809", toConfigurationProperties(config.getPgn129809()));
    }

    if (config.getPgn129810() != null) {
      properties.put("pgn129810", toConfigurationProperties(config.getPgn129810()));
    }

    return properties;
  }

  private static ConfigurationProperties toConfigurationProperties(N2KPgnTransmitConfigDTO config) {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", config.isEnabled());
    properties.put("intervalMilliseconds", config.getIntervalMilliseconds());
    return properties;
  }
}