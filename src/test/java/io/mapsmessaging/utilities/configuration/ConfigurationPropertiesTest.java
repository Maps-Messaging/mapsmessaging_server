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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationPropertiesTest {

  @Test
  void getProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("notEmpty", "value");
    ConfigurationProperties properties = new ConfigurationProperties(props);
    assertNull(properties.getProperty("empty"));
    assertNotNull(properties.getProperty("notEmpty"));
    assertEquals("value", properties.getProperty("notEmpty"));
  }

  @Test
  void getBooleanProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("trueStringValue", "true");
    props.put("falseStringValue", "false");
    props.put("trueValue", true);
    props.put("falseValue", false);
    props.put("booleanError", "this is not boolean");
    ConfigurationProperties properties = new ConfigurationProperties(props);

    assertTrue(properties.getBooleanProperty("trueStringValue", false));
    assertTrue(properties.getBooleanProperty("trueValue", false));

    assertFalse(properties.getBooleanProperty("falseStringValue", true));
    assertFalse(properties.getBooleanProperty("falseValue", true));

    assertFalse(properties.getBooleanProperty("booleanError", false));
    assertTrue(properties.getBooleanProperty("empty", true));
  }

  @Test
  void getLongProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("longString", "10");
    props.put("long", 10);
    props.put("longPowerK", "10K");
    props.put("longPowerM", "10M");
    props.put("longPowerG", "10G");
    props.put("longPowerT", "10T");
    props.put("longError", "10errorf");
    ConfigurationProperties properties = new ConfigurationProperties(props);

    assertEquals(10, properties.getLongProperty("longString", 0));
    assertEquals(10, properties.getLongProperty("long", 0));

    assertEquals(10L*1024L, properties.getLongProperty("longPowerK", 0));
    assertEquals(10L*1024L*1024L, properties.getLongProperty("longPowerM", 0));
    assertEquals(10L*1024L*1024L*1024L, properties.getLongProperty("longPowerG", 0));
    assertEquals(10L*1024L*1024L*1024L*1024L, properties.getLongProperty("longPowerT", 0));

    assertEquals(123, properties.getLongProperty("longError", 123));
    assertEquals(123, properties.getLongProperty("empty", 123));
  }

  @Test
  void getIntProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("longString", "10");
    props.put("long", 10);
    props.put("longPowerK", "10K");
    props.put("longPowerM", "10M");
    props.put("longError", "10errorf");
    ConfigurationProperties properties = new ConfigurationProperties(props);

    assertEquals(10, properties.getIntProperty("longString", 0));
    assertEquals(10, properties.getIntProperty("long", 0));

    assertEquals(10L*1024, properties.getIntProperty("longPowerK", 0));
    assertEquals(10L*1024*1024, properties.getIntProperty("longPowerM", 0));
    assertEquals(123, properties.getIntProperty("longError", 123));
    assertEquals(123, properties.getIntProperty("empty", 123));
  }

  @Test
  void getFloatProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("floatString", "10.5");
    props.put("float", 10.5f);
    props.put("floatError", "10.5errorf");
    ConfigurationProperties properties = new ConfigurationProperties(props);

    assertEquals(10.5f, properties.getFloatProperty("floatString", 0));
    assertEquals(10.5f, properties.getFloatProperty("float", 0));

    assertEquals(11, properties.getIntProperty("floatString", 0)); // Round up
    assertEquals(11, properties.getIntProperty("float", 0));       // Round up

    assertEquals(123.5f, properties.getFloatProperty("floatError", 123.5f));
    assertEquals(123.5f, properties.getFloatProperty("empty", 123.5f));
  }

  @Test
  void getDoubleProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("doubleString", "12.5");
    props.put("double", 12.5);
    props.put("doubleError", "12.5ErrorNumber2");
    ConfigurationProperties properties = new ConfigurationProperties(props);

    assertEquals(12.5f, properties.getDoubleProperty("doubleString", 0));
    assertEquals(12.5f, properties.getDoubleProperty("double", 0));

    assertEquals(13, properties.getIntProperty("doubleString", 0)); // Round up
    assertEquals(13, properties.getIntProperty("double", 0));       // Round up

    assertEquals(123.5, properties.getDoubleProperty("doubleError", 123.5));
    assertEquals(123.5, properties.getDoubleProperty("empty", 123.5));
  }

  @Test
  void containsKey() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("notEmpty", "value");
    assertTrue(properties.containsKey("notEmpty"));
    assertFalse(properties.containsKey("empty"));
  }

  @Test
  void setGlobal() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ConfigurationProperties global = new ConfigurationProperties();
    properties.setGlobal(global);
    assertEquals(global, properties.getGlobal());
  }

  @Test
  void getGlobal() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ConfigurationProperties global = new ConfigurationProperties();
    properties.setGlobal(global);
    global.put("globalLong", "10");
    assertEquals(global, properties.getGlobal());
    assertEquals(10, properties.getLongProperty("globalLong", 0));
  }
}