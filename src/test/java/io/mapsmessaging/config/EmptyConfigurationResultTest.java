/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.rest.auth.RestAccessControl;
import org.junit.jupiter.api.Test;
import java.util.Map;
import javax.security.auth.Subject;
import static org.junit.jupiter.api.Assertions.*;

class EmptyConfigurationResultTest {

  @Test
  void lora_configuration_results_are_fresh_and_mutable() {
    LoRaDeviceManagerConfig config = new LoRaDeviceManagerConfig();
    ConfigurationProperties first = config.toConfigurationProperties();
    ConfigurationProperties second = config.toConfigurationProperties();
    assertNotSame(first, second);
    assertTrue(first.getMap().isEmpty());
    first.put("example", "value");
    assertEquals("value", first.getProperty("example"));
    assertTrue(second.getMap().isEmpty());
  }

  @Test
  void access_maps_are_fresh_mutable_and_preserve_insertion_order() {
    RestAccessControl control = new RestAccessControl();
    Map<String, String> first = control.getAccess(new Subject());
    Map<String, String> second = control.getAccess(new Subject());
    assertNotSame(first, second);
    assertTrue(first.isEmpty());
    first.put("second", "2");
    first.put("first", "1");
    assertEquals(java.util.List.of("second", "first"), java.util.List.copyOf(first.keySet()));
    assertTrue(second.isEmpty());
  }
}
