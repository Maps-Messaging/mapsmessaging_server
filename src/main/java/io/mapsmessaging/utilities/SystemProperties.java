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

package io.mapsmessaging.utilities;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import static io.mapsmessaging.logging.ServerLogMessages.CONFIG_PROPERTY_ACCESS;

/**
 * This class represents a utility class for accessing system properties.
 * It provides methods for retrieving properties, such as strings, booleans, longs, and doubles,
 * from the system properties or environment variables.
 * The class also logs the access to the properties using a logger.
 */

@SuppressWarnings("java:S6548") // yes it is a singleton
public class SystemProperties {

  private static class Holder {
    static final SystemProperties INSTANCE = new SystemProperties();
  }

  public static SystemProperties getInstance() {
    return Holder.INSTANCE;
  }


  private final Logger logger = LoggerFactory.getLogger(SystemProperties.class);

  /**
   * Private constructor for the SystemProperties class.
   * It is used to prevent the instantiation of the SystemProperties class from outside the class itself.
   */
  private SystemProperties() {
  }

  /**
   * Retrieves the value of a system property or environment variable based on the provided key.
   * If the property is not found in the system properties, it checks the environment variables.
   * If the property is still not found, it returns the provided default value.
   *
   * @param key The key of the property to retrieve.
   * @param defaultValue The default value to return if the property is not found.
   * @return The value of the property if found, otherwise the default value.
   */
  public String locateProperty(String key, String defaultValue) {
    String response = getProperty(key);
    if (response == null) {
      response = getEnvProperty(key);
    }
    if (response == null) {
      return defaultValue;
    }
    return response;
  }

  /**
   * Retrieves the value of a system property based on the provided key.
   *
   * @param key The key of the property to retrieve.
   * @return The value of the property if found, otherwise null.
   */
  public String getProperty(String key) {
    return getProperty(key, null);
  }

  /**
   * Retrieves the value of a system property based on the provided key.
   *
   * @param key The key of the property to retrieve.
   * @param defaultValue The default value to return if the property is not found.
   * @return The value of the property if found, otherwise the default value.
   */
  public String getProperty(String key, String defaultValue) {
    String value = System.getProperty(key);
    if (value == null || value.isEmpty()) {
      value = defaultValue;
    }
    logger.log(CONFIG_PROPERTY_ACCESS, key, defaultValue);
    return value;
  }

  /**
   * Retrieves the value of a boolean system property based on the provided key.
   *
   * @param key The key of the property to retrieve.
   * @param defaultValue The default value to return if the property is not found or is empty.
   * @return The boolean value of the property if found, otherwise the default value.
   */
  public boolean getBooleanProperty(String key, boolean defaultValue) {
    String value = System.getProperty(key);
    boolean result = defaultValue;
    if (value != null && !value.isEmpty()) {
      result = Boolean.parseBoolean(value);
    }
    logger.log(CONFIG_PROPERTY_ACCESS, key, result);
    return result;
  }

  /**
   * Retrieves the value of a long system property based on the provided key.
   *
   * @param key The key of the property to retrieve.
   * @param defaultValue The default value to return if the property is not found or is not a valid long.
   * @return The long value of the property if found and is a valid long, otherwise the default value.
   */
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

  /**
   * Retrieves the value of a double system property based on the provided key.
   *
   * @param key The key of the property to retrieve.
   * @param defaultValue The default value to return if the property is not found or is not a valid double.
   * @return The double value of the property if found and is a valid double, otherwise the default value.
   */
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

  /**
   * Retrieves the value of an environment variable based on the provided key.
   *
   * @param key The key of the environment variable to retrieve.
   * @return The value of the environment variable if found, otherwise null.
   */
  public String getEnvProperty(String key) {
    String value = System.getenv(key);
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return null;
  }

}
