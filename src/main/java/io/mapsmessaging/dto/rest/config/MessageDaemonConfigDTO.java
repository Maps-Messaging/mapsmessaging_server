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

package io.mapsmessaging.dto.rest.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Message Daemon Configuration DTO")
public class MessageDaemonConfigDTO extends BaseConfigDTO implements ConfigurationManagerDTO {

  @Schema(
      description = "Interval for delayed publish in milliseconds",
      example = "1000",
      defaultValue = "1000",
      minimum = "500",
      maximum = "60000"
  )
  protected int delayedPublishInterval = 1000;

  @Schema(
      description = "Number of session pipelines",
      example = "48",
      defaultValue = "48",
      minimum = "1",
      maximum = "255"
  )
  protected int sessionPipeLines = 48;

  @Schema(
      description = "Transaction expiry in milliseconds",
      example = "3600000",
      defaultValue = "3600000",
      minimum = "60000",
      maximum = "2419200000"
  )
  protected long transactionExpiry = 3600000L;

  @Schema(
      description = "Transaction scan interval in milliseconds",
      example = "5000",
      defaultValue = "5000",
      minimum = "1000",
      maximum = "30000"
  )
  protected long transactionScan = 5000L;

  @Schema(
      description = "Compression algorithm name",
      example = "None",
      defaultValue = "None"
  )
  protected String compressionName = "None";

  @Schema(
      description = "Minimum size for message compression",
      example = "1024",
      defaultValue = "1024",
      minimum = "128",
      maximum = "4096"
  )
  protected int compressMessageMinSize = 1024;

  @Schema(
      description = "On rollback of events, whether to maintain the priority or increment it",
      example = "maintain",
      defaultValue = "maintain"
  )
  protected String incrementPriorityMethod = "maintain";

  @Schema(
      description = "Enable resource statistics",
      example = "false",
      defaultValue = "false"
  )
  protected boolean enableResourceStatistics = false;

  @Schema(
      description = "Enable system topics",
      example = "true",
      defaultValue = "true"
  )
  protected boolean enableSystemTopics = true;

  @Schema(
      description = "Enable system status topics",
      example = "true",
      defaultValue = "true"
  )
  protected boolean enableSystemStatusTopics = true;

  @Schema(
      description = "Enable system topic averages",
      example = "false",
      defaultValue = "false"
  )
  protected boolean enableSystemTopicAverages = false;

  @Schema(
      description = "Enable JMX monitoring",
      example = "false",
      defaultValue = "false"
  )
  protected boolean enableJMX = false;

  @Schema(
      description = "Enable JMX statistics",
      example = "false",
      defaultValue = "false"
  )
  protected boolean enableJMXStatistics = false;

  @Schema(
      description = "Tag metadata for messages",
      example = "false",
      defaultValue = "false"
  )
  protected boolean tagMetaData = false;

  @Schema(
      description = "Latitude for the daemon location",
      example = "0.0",
      defaultValue = "0.0",
      minimum = "-90.0",
      maximum = "90.0"
  )
  protected double latitude = 0.0;

  @Schema(
      description = "Longitude for the daemon location",
      example = "0.0",
      defaultValue = "0.0",
      minimum = "-180.0",
      maximum = "180.0"
  )
  protected double longitude = 0.0;

  @Schema(
      description = "Send anonymous server usage statistics to Maps Messaging",
      example = "true",
      defaultValue = "true"
  )
  protected boolean sendAnonymousStatusUpdates = true;


  @Schema(
      description = "Exit server startup if invalid configuration detected",
      example = "true",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean exitOnConfigError = false;

}
