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

package io.mapsmessaging.network.protocol.impl.n2k.msg.source;

public class ConfigurationInformationFieldValueSource extends AbstractAisFieldValueSource {

  public ConfigurationInformationFieldValueSource() {
    this(
        "Maps Messaging Server",
        "AIS Source Bridge",
        "MapsMessaging B.V."
    );
  }

  public ConfigurationInformationFieldValueSource(
      String installationDescription1,
      String installationDescription2,
      String manufacturerInformation
  ) {
    putString("installationDescriptionField1", safe(installationDescription1, "Maps Messaging Server"));
    putString("installationDescriptionField2", safe(installationDescription2, "AIS Source Bridge"));
    putString("manufacturerInformationField3", safe(manufacturerInformation, "MapsMessaging B.V."));
  }

  private String safe(String value, String defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return value;
  }
}