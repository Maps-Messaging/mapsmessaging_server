/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slj.mqtt.sn.model.IPreferenceNamespace;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.AbstractMqttsnService;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;

public class MemoryStorage extends AbstractMqttsnService implements IMqttsnStorageService {

  private final Map<String, Object> preferences;

  public MemoryStorage(){
    preferences = new ConcurrentHashMap<>();
  }

  @Override
  public <T> T getPreferenceValue(String s, Class<T> aClass) {
    System.err.println("getPreferenceValue, "+ s);
    return (T) preferences.get(s);
  }

  @Override
  public void setStringPreference(String s, String s1) throws MqttsnException {
    System.err.println("setStringPreference, "+ s+", "+s1);
    preferences.put(s, s1);
  }

  @Override
  public String getStringPreference(String s, String s1) {
    System.err.println("getStringPreference, "+ s+", "+s1);
    return preferences.get(s).toString();
  }

  @Override
  public void setIntegerPreference(String s, Integer integer) throws MqttsnException {
    System.err.println("setIntegerPreference, "+ s+", "+integer);
    preferences.put(s, integer);
  }

  @Override
  public void setLongPreference(String s, Long aLong) throws MqttsnException {
    System.err.println("setLongPreference, "+ s+", "+aLong);
    preferences.put(s, aLong);
  }

  @Override
  public Long getLongPreference(String s, Long aLong) {
    System.err.println("getLongPreference, "+ s+", "+aLong);
    Object o = preferences.get(s);
    if(o instanceof Long){
      return (Long) o;
    }
    else if(o instanceof Number){
      return ((Number)o).longValue();
    }
    return null;
  }

  @Override
  public void setBooleanPreference(String s, Boolean aBoolean) throws MqttsnException {
    System.err.println("setBooleanPreference, "+ s+", "+aBoolean);
    preferences.put(s, aBoolean);
  }

  @Override
  public Boolean getBooleanPreference(String s, Boolean aBoolean) {
    System.err.println("getBooleanPreference, "+ s+", "+aBoolean);
    Object o = preferences.get(s);
    if(o instanceof Boolean){
      return (Boolean)o;
    }
    return aBoolean;
  }

  @Override
  public Integer getIntegerPreference(String s, Integer integer) {
    System.err.println("getIntegerPreference, "+ s+", "+integer);
    Object o = preferences.get(s);
    if(o instanceof Integer){
      return (Integer) o;
    }
    else if(o instanceof Number){
      return ((Number)o).intValue();
    }
    return null;
  }

  @Override
  public void setDatePreference(String s, Date date) throws MqttsnException {
    preferences.put(s, date);
  }

  @Override
  public Date getDatePreference(String s, Date date) {
    System.err.println("getDatePreference, "+ s+", "+date);
    Object o = preferences.get(s);
    if(o instanceof Date){
      return (Date)o;
    }
    return date;
  }

  @Override
  public void saveFile(String s, byte[] bytes) throws MqttsnException {
    System.err.println("Save File");
    // No Op
  }

  @Override
  public Optional<byte[]> loadFileIfExists(String s) throws MqttsnException {
    return Optional.empty();
  }

  @Override
  public void updateRuntimeOptionsFromStorage(MqttsnOptions mqttsnOptions) throws MqttsnException {
  }

  @Override
  public void writeRuntimeOptions(MqttsnOptions mqttsnOptions) throws MqttsnException {

  }

  @Override
  public IMqttsnStorageService getPreferenceNamespace(IPreferenceNamespace iPreferenceNamespace) {
    return null;
  }

  @Override
  public void writeFieldsToStorage(Object o) throws MqttsnRuntimeException {
    // NoOp
  }

  @Override
  public void initializeFieldsFromStorage(Object o) throws MqttsnRuntimeException {
    // NoOp
  }

  @Override
  public File getWorkspaceRoot() {
    return new File(".");
  }
}
