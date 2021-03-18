/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.utilities.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

abstract class PropertyManagerTest {

  protected abstract PropertyManager create();

  @Test
  void load() {
    PropertyManager manager = create();
    assertTrue(manager.properties.isEmpty());
    manager.load();
    assertFalse(manager.properties.isEmpty());
  }

  @Test
  void store() throws IOException {
    PropertyManager manager = create();
    assertTrue(manager.properties.isEmpty());
    manager.load();
    assertFalse(manager.properties.isEmpty());
    File outFile = new File("./configTestFile");
    if(outFile.exists()){
      outFile.delete();
    }
    manager.store(outFile.getAbsolutePath());
    assertTrue(outFile.exists());
    assertTrue(outFile.delete());
  }

  @Test
  void copy() {
    PropertyManager manager1 = create();
    PropertyManager manager2 = create();
    assertTrue(manager1.properties.isEmpty());
    manager1.load();
    assertFalse(manager1.properties.isEmpty());

    assertTrue(manager2.properties.isEmpty());
    manager2.copy(manager1);
    assertFalse(manager2.properties.isEmpty());
  }

  @Test
  void getPropertiesJSON() {
    PropertyManager manager = create();
    assertTrue(manager.properties.isEmpty());
    manager.load();
    assertFalse(manager.properties.isEmpty());
    for(String key:manager.properties.keySet()) {
      JSONObject jsonObject = manager.getPropertiesJSON(key);
      assertNotNull(jsonObject);
    }
  }

  @Test
  void loadPropertiesJSON() {
    PropertyManager manager = create();
    assertTrue(manager.properties.isEmpty());
    manager.load();
    assertFalse(manager.properties.isEmpty());
    List<String> keys = new ArrayList<>(manager.properties.keySet());
    for(String key:keys) {
      JSONObject jsonObject = manager.getPropertiesJSON(key);
      assertNotNull(jsonObject);
      if(!jsonObject.isEmpty()) {
        manager.loadPropertiesJSON(key, jsonObject);
      }
    }
  }

  @Test
  void getProperties() {
    PropertyManager manager = create();
    assertTrue(manager.properties.isEmpty());
    manager.load();
    assertFalse(manager.properties.isEmpty());
    for(String key:manager.properties.keySet()) {
      ConfigurationProperties config = manager.getProperties(key);
      assertNotNull(config);
    }
  }
}