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

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class N2kProtocolConfigTest {

  @Test
  void defaultModelIdIsUsedWhenNotConfigured() {
    ConfigurationProperties props = new ConfigurationProperties();
    N2kProtocolConfig config = new N2kProtocolConfig(props);
    assertEquals("Maps Messaging Server", config.getModelId());
  }

  @Test
  void defaultModelSerialCodeIsUsedWhenNotConfigured() {
    ConfigurationProperties props = new ConfigurationProperties();
    N2kProtocolConfig config = new N2kProtocolConfig(props);
    assertEquals("unknown", config.getModelSerialCode());
  }

  @Test
  void customModelIdIsReadFromConfig() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("modelId", "My Custom Device");
    N2kProtocolConfig config = new N2kProtocolConfig(props);
    assertEquals("My Custom Device", config.getModelId());
  }

  @Test
  void customModelSerialCodeIsReadFromConfig() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("modelSerialCode", "MAPS-CMRE-001");
    N2kProtocolConfig config = new N2kProtocolConfig(props);
    assertEquals("MAPS-CMRE-001", config.getModelSerialCode());
  }

  @Test
  void toConfigurationPropertiesRoundTripsModelFields() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("modelId", "Test Device");
    props.put("modelSerialCode", "SN-12345");
    N2kProtocolConfig config = new N2kProtocolConfig(props);
    ConfigurationProperties out = config.toConfigurationProperties();
    assertEquals("Test Device", out.getProperty("modelId"));
    assertEquals("SN-12345", out.getProperty("modelSerialCode"));
  }
}
