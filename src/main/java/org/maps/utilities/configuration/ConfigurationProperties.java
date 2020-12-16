/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.utilities.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationProperties extends java.util.Properties {

  protected ConfigurationProperties(HashMap<String, String> list) {
    for (Map.Entry<String, String> entry : list.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  protected ConfigurationProperties(String resourceName) throws IOException {
    String propResourceName = "/" + resourceName;
    while (propResourceName.contains(".")) {
      propResourceName = propResourceName.replace('.', File.separatorChar);
    }
    propResourceName = propResourceName + ".props";
    InputStream is = getClass().getResourceAsStream(propResourceName);
    if (is != null) {
      load(is);
    } else {
      throw new FileNotFoundException("No such resource found " + propResourceName);
    }
  }

  public boolean getBooleanProperty(String key, boolean defaultValue) {
    String val = getProperty(key, "" + defaultValue).trim();
    return Boolean.parseBoolean(val);
  }

  public long getLongProperty(String key, long defaultValue) {
    try {
      String val = getProperty(key, "" + defaultValue).trim();
      return asLong(val);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public int getIntProperty(String key, int defaultValue) {
    try {
      String val = getProperty(key, "" + defaultValue).trim();
      return (int) asLong(val);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public float getFloatProperty(String key, float defaultValue) {
    try {
      String val = getProperty(key, "" + defaultValue).trim();
      return (float) asDouble(val);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private long asLong(String entry) {
    long multiplier = 1L;
    String value = entry.trim();
    String end = value.substring(value.length() - 1);

    if (end.equalsIgnoreCase("T")) {
      multiplier = 1024L * 1024L * 1024L * 1024L;
    } else if (end.equalsIgnoreCase("G")) {
      multiplier = 1024L * 1024L * 1024L;
    } else if (end.equalsIgnoreCase("M")) {
      multiplier = 1024L * 1024L;
    } else if (end.equalsIgnoreCase("K")) {
      multiplier = 1024;
    }
    if (multiplier > 1) {
      value = value.substring(0, value.length() - 1);
    }
    long val = Long.parseLong(value);
    val = val * multiplier;
    return val;
  }

  private double asDouble(String entry) {
    return Double.parseDouble(entry);
  }
}
