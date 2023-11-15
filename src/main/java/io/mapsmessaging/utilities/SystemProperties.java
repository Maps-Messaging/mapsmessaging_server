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

public class SystemProperties {


  public static String getProperty(String key) {
    return getProperty(key, null);
  }

  public static String getProperty(String key, String defaultValue) {
    String value = System.getProperty(key);
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return defaultValue;
  }

  public static boolean getBooleanProperty(String key, boolean defaultValue) {
    String value = System.getProperty(key);
    if (value != null && !value.isEmpty()) {
      return Boolean.parseBoolean(value);
    }
    return defaultValue;
  }

  public static long getLongProperty(String key, long defaultValue) {
    String value = System.getProperty(key);
    if (value != null && !value.isEmpty()) {
      try {
        return Long.parseLong(value);
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    return defaultValue;
  }

  public static double getDoubleProperty(String key, double defaultValue) {
    String value = System.getProperty(key);
    if (value != null && !value.isEmpty()) {
      try {
        return Double.parseDouble(value);
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    return defaultValue;
  }

  public static String getEnvProperty(String key, String defaultValue) {
    String value = System.getenv(key);
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return defaultValue;
  }


}
