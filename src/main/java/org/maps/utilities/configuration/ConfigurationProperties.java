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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.json.JSONObject;

public class ConfigurationProperties extends java.util.Properties {

  private Map<String, String> globalValues = new HashMap<>();

  public ConfigurationProperties() {
  }

  protected ConfigurationProperties(Map<String, String> list, @Nullable Map<String, String> global) {
    for (Map.Entry<String, String> entry : list.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
    if(global != null) {
      this.globalValues = global;
    }
  }

  public Map<String, String> getGlobalValues(){
    return new LinkedHashMap<>(globalValues);
  }

  public JSONObject toJSON(){
    JSONObject object = new JSONObject();
    Set<Entry<Object, Object>> entries = super.entrySet();

    for(Entry<Object, Object> entry:entries){
      object.put(entry.getKey().toString(), entry.getValue().toString());
    }
    return object;
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    String val;
    if(containsKey(key)){
      val = super.getProperty(key);
    }
    else {
      val = globalValues.getOrDefault(key, defaultValue);
    }
    return val;
  }

  @Override
  public String getProperty(String key) {
    String val;
    if(containsKey(key)){
      val = super.getProperty(key);
    }
    else {
      val = globalValues.get(key);
    }
    return val;
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
