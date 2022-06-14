/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

import io.mapsmessaging.consul.ConsulManagerFactory;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsulPropertyManagerTest {

  @BeforeEach
  public void beforeMethod() {
    ConsulManagerFactory.getInstance().start(UUID.randomUUID());
    if(ConsulManagerFactory.getInstance().isStarted()) {
      ConsulManagerFactory.getInstance().getManager();
    }
    Assumptions.assumeTrue(ConsulManagerFactory.getInstance().getManager() != null);
  }

  @Test
  void load() {
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("storeTest");
    propertyManager.load();
    System.err.println(propertyManager.toString());
  }

  @Test
  void store() {
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("storeTest");
    ConfigurationProperties properties = new ConfigurationProperties();
    for(int x=0;x<100;x++) {
      properties.put("key"+x, "value"+x);
    }
    propertyManager.properties.put("data", properties);
    propertyManager.store("data");
    propertyManager.load();
    System.err.println(propertyManager.toString());
  }

  @Test
  void copy() {

  }

  @Test
  void save() {

  }
}