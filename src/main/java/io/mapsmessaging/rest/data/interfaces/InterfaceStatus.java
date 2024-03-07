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
import io.mapsmessaging.network.io.connection.EndPointConnection;
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
  private String interfaceName;
  @Schema(description = "Number of bytes sent")
  private long bytesSent;
  @Schema(description = "Number of bytes received")
  private long bytesReceived;
  @Schema(description = "Number of messages sent")
  private long messagesSent;
  @Schema(description = "Number of messages received")
  private long messagesReceived;
  @Schema(description = "Number current connections")
  private long connections;

  @Schema(description = "Number connection errors")
  private long errors;

  @Schema(description = "Map of moving averages")
  private Map<String, LinkedMovingAverageRecord> statistics;

  public InterfaceStatus() {

  }

  public InterfaceStatus(EndPointServer server){
    interfaceName = server.getConfigName();
    bytesSent = server.getTotalBytesSent();
    bytesReceived = server.getTotalBytesRead();
    messagesSent = server.getTotalPacketsSent();
    messagesReceived = server.getTotalPacketsRead();
    connections = server.size();
    statistics = new LinkedHashMap<>();
    errors = server.getTotalErrors();
    addToMap(statistics, server.getAverageBytesRead());
    addToMap(statistics, server.getAverageBytesSent());
    addToMap(statistics, server.getAveragePacketsRead());
    addToMap(statistics, server.getAveragePacketsSent());
  }

  public InterfaceStatus(EndPointConnection endPointConnection) {
    interfaceName = endPointConnection.getConfigName();
    bytesSent = endPointConnection.getTotalBytesSent();
    bytesReceived = endPointConnection.getTotalBytesRead();
    messagesSent = endPointConnection.getTotalPacketsSent();
    messagesReceived = endPointConnection.getTotalPacketsRead();
    connections = 1;
    statistics = new LinkedHashMap<>();
    errors = endPointConnection.getTotalErrors();
    addToMap(statistics, endPointConnection.getAverageBytesRead());
    addToMap(statistics, endPointConnection.getAverageBytesSent());
    addToMap(statistics, endPointConnection.getAveragePacketsRead());
    addToMap(statistics, endPointConnection.getAveragePacketsSent());
  }

  private void addToMap(Map<String, LinkedMovingAverageRecord> stats, LinkedMovingAverageRecord average){
    stats.put(average.getName(), average);
  }

}
