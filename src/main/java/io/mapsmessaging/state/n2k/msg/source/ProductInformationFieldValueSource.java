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

package io.mapsmessaging.state.n2k.msg.source;

public class ProductInformationFieldValueSource extends AbstractAisFieldValueSource {

  private static final double DEFAULT_NMEA2000_VERSION = 2.100;
  private static final long DEFAULT_PRODUCT_CODE = 1L;
  private static final long DEFAULT_CERTIFICATION_LEVEL = 0L;
  private static final long DEFAULT_LOAD_EQUIVALENCY = 1L;

  public ProductInformationFieldValueSource(
      String modelId,
      String softwareVersionCode,
      String modelVersion,
      String modelSerialCode
  ) {
    this(
        DEFAULT_NMEA2000_VERSION,
        DEFAULT_PRODUCT_CODE,
        modelId,
        softwareVersionCode,
        modelVersion,
        modelSerialCode,
        DEFAULT_CERTIFICATION_LEVEL,
        DEFAULT_LOAD_EQUIVALENCY
    );
  }

  public ProductInformationFieldValueSource(
      double nmea2000Version,
      long productCode,
      String modelId,
      String softwareVersionCode,
      String modelVersion,
      String modelSerialCode,
      long certificationLevel,
      long loadEquivalency
  ) {
    putDouble("nmea2000DatabaseVersion", nmea2000Version);
    putLong("nmeaManufacturersProductCode", productCode);
    putString("manufacturersModelId", safe(modelId, "Maps Messaging Server"));
    putString("manufacturersSoftwareVersionCode", safe(softwareVersionCode, "unknown"));
    putString("manufacturersModelVersion", safe(modelVersion, "1.0"));
    putString("manufacturersModelSerialCode", safe(modelSerialCode, "unknown"));
    putLong("nmea2000CertificationLevel", certificationLevel);
    putLong("loadEquivalency", loadEquivalency);
  }

  private String safe(String value, String defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return value;
  }
}