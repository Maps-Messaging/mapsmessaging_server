/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.utilities;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import lombok.Getter;

import static io.mapsmessaging.logging.ServerLogMessages.CONFIG_PROPERTY_ACCESS;

public class SystemProperties {

  static {
    instance = new SystemProperties();
  }

  @Getter
  private static final SystemProperties instance;

  private final Logger logger = LoggerFactory.getLogger(SystemProperties.class);

  private SystemProperties() {
  }


  public String getProperty(String key) {
    return getProperty(key, null);
  }

  public String getProperty(String key, String defaultValue) {
    String value = System.getProperty(key);
    if (value == null || value.isEmpty()) {
      value = defaultValue;
    }
    logger.log(CONFIG_PROPERTY_ACCESS, key, defaultValue);
    return value;
  }

  public boolean getBooleanProperty(String key, boolean defaultValue) {
    String value = System.getProperty(key);
    boolean result = defaultValue;
    if (value != null && !value.isEmpty()) {
      result = Boolean.parseBoolean(value);
    }
    logger.log(CONFIG_PROPERTY_ACCESS, key, result);
    return result;
  }

  public long getLongProperty(String key, long defaultValue) {
    String value = System.getProperty(key);
    long result = defaultValue;
    if (value != null && !value.isEmpty()) {
      try {
        result = Long.parseLong(value);
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    logger.log(CONFIG_PROPERTY_ACCESS, key, result);
    return result;
  }

  public double getDoubleProperty(String key, double defaultValue) {
    String value = System.getProperty(key);
    double result = defaultValue;
    if (value != null && !value.isEmpty()) {
      try {
        result = Double.parseDouble(value);
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    logger.log(CONFIG_PROPERTY_ACCESS, key, result);
    return result;
  }

  public String getEnvProperty(String key) {
    String value = System.getenv(key);
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return null;
  }

}
