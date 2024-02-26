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

package io.mapsmessaging.rest.data.interfaces;

import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.utilities.stats.LinkedMovingAverageRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ToString
public class InterfaceStatus {
  @Schema(description = "Name of the interface")
  private final String name;
  @Schema(description = "Number of bytes sent")
  private final long bytesSent;
  @Schema(description = "Number of bytes received")
  private final long bytesReceived;
  @Schema(description = "Number of messages sent")
  private final long messagesSent;
  @Schema(description = "Number of messages received")
  private final long messagesReceived;
  @Schema(description = "Map of moving averages")
  private final Map<String, LinkedMovingAverageRecord> statistics;

  public InterfaceStatus(EndPointServer server){
    name = server.getName();
    bytesSent = server.getTotalBytesSent();
    bytesReceived = server.getTotalBytesRead();
    messagesSent = server.getTotalPacketsSent();
    messagesReceived = server.getTotalPacketsRead();
    statistics = new LinkedHashMap<>();
    addToMap(statistics, server.getAverageBytesRead());
    addToMap(statistics, server.getAverageBytesSent());
    addToMap(statistics, server.getAveragePacketsRead());
    addToMap(statistics, server.getAveragePacketsSent());
  }

  private void addToMap(Map<String, LinkedMovingAverageRecord> stats, LinkedMovingAverageRecord average){
    stats.put(average.getName(), average);
  }

}
