/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.dto.helpers;

import io.mapsmessaging.dto.rest.ServerStatisticsDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;

public class ServerStatisticsHelper {
  public static ServerStatisticsDTO create() {
    return new ServerStatisticsDTO(
        EndPointServerStatus.SystemTotalPacketsSent.sum(),
        EndPointServerStatus.SystemTotalPacketsReceived.sum(),
        EndPoint.totalReadBytes.sum(),
        EndPoint.totalWriteBytes.sum(),
        EndPoint.totalConnections.sum(),
        EndPoint.totalDisconnections.sum(),
        DestinationImpl.getGlobalStats().getTotalNoInterestMessages(),
        DestinationImpl.getGlobalStats().getTotalSubscribedMessages(),
        DestinationImpl.getGlobalStats().getTotalPublishedMessages(),
        DestinationImpl.getGlobalStats().getTotalRetrievedMessages(),
        DestinationImpl.getGlobalStats().getTotalExpiredMessages(),
        DestinationImpl.getGlobalStats().getTotalDeliveredMessages(),
        DestinationImpl.getGlobalStats().getPublishedPerSecond(),
        DestinationImpl.getGlobalStats().getSubscribedPerSecond(),
        DestinationImpl.getGlobalStats().getNoInterestPerSecond(),
        DestinationImpl.getGlobalStats().getDeliveredPerSecond(),
        DestinationImpl.getGlobalStats().getRetrievedPerSecond(),
        DestinationImpl.getGlobalStats().getGlobalStats());
  }

  private ServerStatisticsHelper() {
  }
}
