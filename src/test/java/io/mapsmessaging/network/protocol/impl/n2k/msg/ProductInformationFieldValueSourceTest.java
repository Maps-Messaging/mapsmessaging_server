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

package io.mapsmessaging.network.protocol.impl.n2k.msg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductInformationFieldValueSourceTest {

  @Test
  void convenienceConstructorUsesNmeaDefaultsAndSafeStringFallbacks() {
    ProductInformationFieldValueSource source =
        new ProductInformationFieldValueSource(null, " ", "v1", null);

    assertEquals(2.1, source.getDouble("nmea2000DatabaseVersion"), 0.0);
    assertEquals(1L, source.getLong("nmeaManufacturersProductCode"));
    assertEquals("Maps Messaging Server", source.getString("manufacturersModelId"));
    assertEquals("unknown", source.getString("manufacturersSoftwareVersionCode"));
    assertEquals("v1", source.getString("manufacturersModelVersion"));
    assertEquals("unknown", source.getString("manufacturersModelSerialCode"));
    assertEquals(0L, source.getLong("nmea2000CertificationLevel"));
    assertEquals(1L, source.getLong("loadEquivalency"));
  }

  @Test
  void fullConstructorPreservesExplicitValues() {
    ProductInformationFieldValueSource source =
        new ProductInformationFieldValueSource(
            3.0, 42L, "model", "software", "version", "serial", 2L, 3L);

    assertEquals(3.0, source.getDouble("nmea2000DatabaseVersion"), 0.0);
    assertEquals(42L, source.getLong("nmeaManufacturersProductCode"));
    assertEquals("model", source.getString("manufacturersModelId"));
    assertEquals("software", source.getString("manufacturersSoftwareVersionCode"));
    assertEquals("version", source.getString("manufacturersModelVersion"));
    assertEquals("serial", source.getString("manufacturersModelSerialCode"));
    assertEquals(2L, source.getLong("nmea2000CertificationLevel"));
    assertEquals(3L, source.getLong("loadEquivalency"));
  }
}
