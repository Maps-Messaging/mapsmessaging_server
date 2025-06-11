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

package io.mapsmessaging.dto.helpers;

import io.mapsmessaging.dto.rest.interfaces.InterfaceStatusDTO;
import io.mapsmessaging.dto.rest.stats.LinkedMovingAverageRecordDTO;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.connection.EndPointConnection;

import java.util.LinkedHashMap;
import java.util.Map;

public class InterfaceStatusHelper {

  public static InterfaceStatusDTO fromServer(EndPointServer server) {
    InterfaceStatusDTO dto = new InterfaceStatusDTO();
    dto.setInterfaceName(server.getConfigName());
    dto.setTotalBytesSent(server.getTotalBytesSent());
    dto.setTotalBytesReceived(server.getTotalBytesRead());
    dto.setTotalMessagesSent(server.getTotalPacketsSent());
    dto.setTotalMessagesReceived(server.getTotalPacketsRead());
    dto.setBytesSent(server.getBytesSentPerSecond());
    dto.setBytesReceived(server.getBytesReadPerSecond());
    dto.setMessagesReceived(server.getMessagesReadPerSecond());
    dto.setMessagesSent(server.getMessagesSentPerSecond());
    dto.setConnections(server.size());
    dto.setErrors(server.getTotalErrors());

    Map<String, LinkedMovingAverageRecordDTO> stats = new LinkedHashMap<>();
    addToMap(stats, server.getAverageBytesRead());
    addToMap(stats, server.getAverageBytesSent());
    addToMap(stats, server.getAveragePacketsRead());
    addToMap(stats, server.getAveragePacketsSent());
    dto.setStatistics(stats);

    return dto;
  }

  public static InterfaceStatusDTO fromConnection(EndPointConnection endPointConnection) {
    InterfaceStatusDTO dto = new InterfaceStatusDTO();
    dto.setInterfaceName(endPointConnection.getConfigName());
    dto.setTotalBytesSent(endPointConnection.getTotalBytesSent());
    dto.setTotalBytesReceived(endPointConnection.getTotalBytesRead());
    dto.setTotalMessagesSent(endPointConnection.getTotalPacketsSent());
    dto.setTotalMessagesReceived(endPointConnection.getTotalPacketsRead());
    dto.setBytesSent(endPointConnection.getBytesSentPerSecond());
    dto.setBytesReceived(endPointConnection.getBytesReadPerSecond());
    dto.setMessagesReceived(endPointConnection.getMessagesReadPerSecond());
    dto.setMessagesSent(endPointConnection.getMessagesSentPerSecond());
    dto.setConnections(1);
    dto.setErrors(endPointConnection.getTotalErrors());

    Map<String, LinkedMovingAverageRecordDTO> stats = new LinkedHashMap<>();
    addToMap(stats, endPointConnection.getAverageBytesRead());
    addToMap(stats, endPointConnection.getAverageBytesSent());
    addToMap(stats, endPointConnection.getAveragePacketsRead());
    addToMap(stats, endPointConnection.getAveragePacketsSent());
    dto.setStatistics(stats);

    return dto;
  }

  private static void addToMap(
      Map<String, LinkedMovingAverageRecordDTO> stats, LinkedMovingAverageRecordDTO average) {
    if (average != null) {
      stats.put(average.getName(), average);
    }
  }

  private InterfaceStatusHelper() {
  }

}
