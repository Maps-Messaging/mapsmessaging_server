/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.rest.data.integration;

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.utilities.stats.LinkedMovingAverageRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class IntegrationStatus {
  @Schema(description = "Name of the interface")
  private String interfaceName;
  @Schema(description = "Number of bytes sent")
  private long bytesSent;
  @Schema(description = "Number of bytes received")
  private long bytesReceived;
  @Schema(description = "Number of messages sent")
  private long messagesSent;
  @Schema(description = "Number of messages received")
  private long messagesReceived;
  @Schema(description = "Number connection errors")
  private long errors;

  @Schema(description = "Current state of the interface")
  private String state;

  @Schema(description = "Map of moving averages")
  private Map<String, LinkedMovingAverageRecord> statistics;

  public IntegrationStatus() {
}

  public IntegrationStatus(EndPointConnection connection){
    state = connection.getState().getName();
    interfaceName = connection.getConfigName();
    bytesSent = connection.getTotalBytesSent();
    bytesReceived = connection.getTotalBytesRead();
    messagesSent = connection.getTotalPacketsSent();
    messagesReceived = connection.getTotalPacketsRead();
    statistics = new LinkedHashMap<>();
    errors = connection.getTotalErrors();
    addToMap(statistics, connection.getAverageBytesRead());
    addToMap(statistics, connection.getAverageBytesSent());
    addToMap(statistics, connection.getAveragePacketsRead());
    addToMap(statistics, connection.getAveragePacketsSent());
  }

  private void addToMap(Map<String, LinkedMovingAverageRecord> stats, LinkedMovingAverageRecord average){
    stats.put(average.getName(), average);
  }

}
