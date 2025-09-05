/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
public class MessageDaemonConfigDTO extends BaseConfigDTO {

  @Schema(description = "Interval for delayed publish in milliseconds", example = "1000")
  protected int delayedPublishInterval;

  @Schema(description = "Number of session pipelines", example = "48")
  protected int sessionPipeLines;

  @Schema(description = "Transaction expiry in milliseconds", example = "3600000")
  protected long transactionExpiry;

  @Schema(description = "Transaction scan interval in milliseconds", example = "5000")
  protected long transactionScan;

  @Schema(description = "Compression algorithm name", example = "None", allowableValues = {"inflator", "none"})
  protected String compressionName;

  @Schema(description = "Minimum size for message compression", example = "1024")
  protected int compressMessageMinSize;

  @Schema(description = "On rollback of events if we maintain the priority or bump the priority of the event", example = "maintain", allowableValues = {"maintain", "increment"})
  protected String incrementPriorityMethod;

  @Schema(description = "Enable resource statistics", example = "false")
  protected boolean enableResourceStatistics;

  @Schema(description = "Enable system topics", example = "true")
  protected boolean enableSystemTopics;

  @Schema(description = "Enable system status topics", example = "true")
  protected boolean enableSystemStatusTopics;

  @Schema(description = "Enable system topic averages", example = "false")
  protected boolean enableSystemTopicAverages;

  @Schema(description = "Enable JMX monitoring", example = "false")
  protected boolean enableJMX;

  @Schema(description = "Enable JMX statistics", example = "false")
  protected boolean enableJMXStatistics;

  @Schema(description = "Tag metadata for messages", example = "false")
  protected boolean tagMetaData;

  @Schema(description = "Latitude for the daemon location", example = "0.0")
  protected double latitude;

  @Schema(description = "Longitude for the daemon location", example = "0.0")
  protected double longitude;

  @Schema(description = "Send anonymous server usage statistics to Maps Messaging", example = "false")
  protected boolean sendAnonymousStatusUpdates;

}
