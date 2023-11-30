/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.rest.data;

import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.utilities.stats.LinkedMovingAverageRecord;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ServerStatistics implements Serializable {

  private final long packetsSent;
  private final long packetsReceived;
  private final long totalReadBytes;
  private final long totalWriteBytes;
  private final long totalConnections;
  private final long totalDisconnections;
  private final long totalNoInterestMessages;
  private final long totalSubscribedMessages;
  private final long totalPublishedMessages;
  private final long totalRetrievedMessages;
  private final long totalExpiredMessages;
  private final long totalDeliveredMessages;
  private final Map<String, LinkedMovingAverageRecord> stats;


  public ServerStatistics() {
    packetsReceived = EndPointServerStatus.SystemTotalPacketsReceived.sum();
    packetsSent = EndPointServerStatus.SystemTotalPacketsSent.sum();

    totalConnections = EndPoint.totalConnections.sum();
    totalReadBytes = EndPoint.totalReadBytes.sum();
    totalWriteBytes = EndPoint.totalWriteBytes.sum();
    totalDisconnections = EndPoint.totalDisconnections.sum();


    totalNoInterestMessages = DestinationStats.getTotalNoInterestMessages();
    totalSubscribedMessages = DestinationStats.getTotalSubscribedMessages();
    totalPublishedMessages = DestinationStats.getTotalPublishedMessages();
    totalRetrievedMessages = DestinationStats.getTotalRetrievedMessages();
    totalDeliveredMessages = DestinationStats.getTotalDeliveredMessages();
    totalExpiredMessages = DestinationStats.getTotalExpiredMessages();
    stats = DestinationStats.getGlobalStats();
  }
}
