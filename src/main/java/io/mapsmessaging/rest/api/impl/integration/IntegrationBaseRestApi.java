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

package io.mapsmessaging.rest.api.impl.integration;

import io.mapsmessaging.dto.rest.integration.IntegrationStatusDTO;
import io.mapsmessaging.dto.rest.stats.LinkedMovingAverageRecordDTO;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.api.impl.BaseRestApi;

import java.util.LinkedHashMap;
import java.util.Map;

public class IntegrationBaseRestApi extends BaseRestApi {
  protected static final String RESOURCE = "integration";

  public IntegrationStatusDTO fromConnection(EndPointConnection connection) {
    IntegrationStatusDTO dto = new IntegrationStatusDTO();
    dto.setState(connection.getState().getName());
    dto.setInterfaceName(connection.getConfigName());
    dto.setBytesSent(connection.getTotalBytesSent());
    dto.setBytesReceived(connection.getTotalBytesRead());
    dto.setMessagesSent(connection.getTotalPacketsSent());
    dto.setMessagesReceived(connection.getTotalPacketsRead());
    dto.setErrors(connection.getTotalErrors());

    // Initialize statistics and timestamps if connection details are available
    Map<String, LinkedMovingAverageRecordDTO> stats = new LinkedHashMap<>();
    if (connection.getProtocol() != null && connection.getProtocol().getEndPoint() != null) {
      dto.setLastReadTime(connection.getProtocol().getEndPoint().getLastRead());
      dto.setLastWriteTime(connection.getProtocol().getEndPoint().getLastWrite());
    }

    // Populate statistics
    addToMap(stats, connection.getAverageBytesRead());
    addToMap(stats, connection.getAverageBytesSent());
    addToMap(stats, connection.getAveragePacketsRead());
    addToMap(stats, connection.getAveragePacketsSent());
    dto.setStatistics(stats);

    return dto;
  }

  private static void addToMap(
      Map<String, LinkedMovingAverageRecordDTO> stats, LinkedMovingAverageRecordDTO average) {
    if (average != null) {
      stats.put(average.getName(), average);
    }
  }
}
