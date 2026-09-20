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
import io.mapsmessaging.dto.rest.config.protocol.impl.CotProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.NmeaConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CotProtocolConfigTest {

  @Test
  void emptyConfigurationUsesDtoDefaults() {
    CotProtocolConfig config = new CotProtocolConfig(new ConfigurationProperties());

    assertEquals("cot", config.getType());
    assertEquals("", config.getTakHostname());
    assertEquals(8088, config.getTakPort());
    assertFalse(config.isTakTlsEnabled());
    assertEquals("TLSv1.2", config.getTakTlsContext());
    assertNull(config.getTakKeyStore());
    assertNull(config.getTakTrustStore());
  }

  @Test
  void configuredValuesAndStoresAreLoadedAndPacked() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("takHostname", "tak.example.org");
    properties.put("takPort", 9000);
    properties.put("takTlsEnabled", true);
    properties.put("takTlsContext", "TLSv1.3");

    ConfigurationProperties keyStore = new ConfigurationProperties();
    keyStore.put("type", "PKCS12");
    keyStore.put("path", "/tmp/client.p12");
    properties.put("takKeyStore", keyStore);

    ConfigurationProperties trustStore = new ConfigurationProperties();
    trustStore.put("type", "JKS");
    trustStore.put("path", "/tmp/trust.jks");
    properties.put("takTrustStore", trustStore);

    CotProtocolConfig config = new CotProtocolConfig(properties);

    assertEquals("tak.example.org", config.getTakHostname());
    assertEquals(9000, config.getTakPort());
    assertTrue(config.isTakTlsEnabled());
    assertEquals("TLSv1.3", config.getTakTlsContext());
    assertEquals("/tmp/client.p12", config.getTakKeyStore().getPath());
    assertEquals("/tmp/trust.jks", config.getTakTrustStore().getPath());

    ConfigurationProperties packed = config.toConfigurationProperties();
    assertEquals("tak.example.org", packed.getProperty("takHostname"));
    assertEquals(9000, packed.getIntProperty("takPort", -1));
    assertTrue(packed.getBooleanProperty("takTlsEnabled", false));
    assertEquals("TLSv1.3", packed.getProperty("takTlsContext"));
    assertTrue(packed.get("takKeyStore") instanceof ConfigurationProperties);
    assertTrue(packed.get("takTrustStore") instanceof ConfigurationProperties);
  }

  @Test
  void updateReportsOnlyActualChanges() {
    CotProtocolConfig config = new CotProtocolConfig(new ConfigurationProperties());
    CotProtocolConfigDTO updated = new CotProtocolConfigDTO();
    updated.setTakHostname("tak.internal");
    updated.setTakPort(9443);
    updated.setTakTlsEnabled(true);
    updated.setTakTlsContext("TLSv1.3");

    assertTrue(config.update(updated));
    assertEquals("tak.internal", config.getTakHostname());
    assertEquals(9443, config.getTakPort());
    assertTrue(config.isTakTlsEnabled());
    assertEquals("TLSv1.3", config.getTakTlsContext());

    assertFalse(config.update(updated));
    assertFalse(config.update(new NmeaConfigDTO()));
  }
}
