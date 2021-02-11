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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationProperties extends LinkedHashMap<String, Object> {

  private ConfigurationProperties globalValues;

  protected ConfigurationProperties(){
    super();
  }

  protected ConfigurationProperties(Map<String, Object> map) {
    for(Map.Entry<String, Object> entry:map.entrySet()){
      if(entry.getValue() instanceof Map){
        put(entry.getKey(), new ConfigurationProperties((Map<String, Object>)entry.getValue()));
      }
      else if(entry.getValue() instanceof List){
        List<Object> parsedList = new ArrayList<>();
        for(Object list:(List<Object>)entry.getValue()){
          if(list instanceof Map){
            parsedList.add(new ConfigurationProperties((Map<String, Object>) list));
          }
          else{
            parsedList.add(list);
          }
        }
        put(entry.getKey(), parsedList);
      }
      else{
        put(entry.getKey(), entry.getValue());
      }
    }

    Object global = map.get("global");
    if(global instanceof ConfigurationProperties){
      globalValues = (ConfigurationProperties) global;
    }
  }
  public String getProperty(String key) {
    return getProperty(key, null);
  }

  public String getProperty(String key, String defaultValue) {
    Object val = get(key, defaultValue);
    if(val != null){
      return val.toString();
    }
    return null;
  }

  public boolean getBooleanProperty(String key, boolean defaultValue) {
    return asBoolean(get(key, defaultValue));
  }

  public long getLongProperty(String key, long defaultValue) {
    try {
      return asLong(get(key,  defaultValue));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public int getIntProperty(String key, int defaultValue) {
    try {
      return (int) asLong(get(key,  defaultValue));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public float getFloatProperty(String key, float defaultValue) {
    try {
      return (float) asDouble(get(key,  defaultValue));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private Object get(String key, Object defaultValue) {
    Object val = get(key);
    if(val == null) {
      val = globalValues.get(key);
    }
    if(val != null){
      return val;
    }
    return defaultValue;
  }

  private boolean asBoolean(Object value){
    if(value instanceof Boolean){
      return (Boolean) value;
    }
    else if(value instanceof String){
      return Boolean.parseBoolean(((String)value).trim());
    }
    return false;
  }

  private long asLong(Object entry) {
    if(entry instanceof Number){
      return  ((Number)entry).longValue();
    }
    else if(entry instanceof String) {
      long multiplier = 1L;
      String value = ((String)entry).trim();
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
    throw new NumberFormatException("Unknown number format detected ["+entry+"]");
  }

  private double asDouble(Object entry) {
    if(entry instanceof Number){
      return ((Number)entry).doubleValue();
    }
    else if(entry instanceof String) {
      return Double.parseDouble( ((String)entry).trim());
    }
    throw new NumberFormatException("Unknown number format detected ["+entry+"]");
  }

  public boolean containsKey(String key) {
    if(!super.containsKey(key)){
      if(globalValues != null) {
        return globalValues.containsKey(key);
      }
      return false;
    }
    return true;
  }

  public void setGlobal(ConfigurationProperties global) {
    this.globalValues = global;
  }
}
