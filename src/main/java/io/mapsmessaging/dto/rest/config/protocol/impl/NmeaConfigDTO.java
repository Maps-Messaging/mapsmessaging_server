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

package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NmeaConfigDTO extends ProtocolConfigDTO {

  public NmeaConfigDTO() {
    super("NMEA-0183");
  }

  @Schema(
      description =
          "Topic name template used when publishing accepted NMEA 0183 sentences. "
              + "Supported placeholders: {deviceName}, {sentence}. "
              + "{deviceName} is populated from the configured source/device name. "
              + "{sentence} is the NMEA sentence identifier, for example GPGGA, GPRMC, or AIVDM.",
      example = "/NMEA0183/{deviceName}/{sentence}",
      defaultValue = "/NMEA0183/{deviceName}/{sentence}",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected String topicNameTemplate = "/NMEA0183/{deviceName}/{sentence}";

  @Schema(
      description = "Controls whether parsed NMEA 0183 sentences are published to the configured topic.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected boolean publish = true;

  @Schema(
      description =
          "Controls whether selected NMEA 0183 position sentences are used to update the server location. "
              + "When enabled, the sentence configured by sentenceForServerLocation is used as the location source.",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected boolean useForServerLocation = false;

  @Schema(
      description =
          "NMEA 0183 sentence identifier used for updating the server location when useForServerLocation is enabled. "
              + "Common values include GGA, GLL, RMC, GPGGA, GNGGA, GPRMC, and GNRMC.",
      example = "GGA",
      defaultValue = "GGA",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected String sentenceForServerLocation = "GGA";

  @Schema(
      description =
          "Output format used when publishing parsed NMEA 0183 sentence data. "
              + "Supported values: json.",
      example = "json",
      defaultValue = "json",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected String format = "json";

  @Schema(
      description = "Serial port configuration used when the NMEA 0183 source is connected over a serial device.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected SerialConfigDTO serial;
}