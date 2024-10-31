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

package io.mapsmessaging.rest.data;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.utilities.stats.LinkedMovingAverageRecord;
import java.io.Serializable;
import java.util.Map;
import lombok.Data;

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

  private final long publishedPerSecond;
  private final long subscribedPerSecond;
  private final long noInterestPerSecond;
  private final long deliveredPerSecond;
  private final long retrievedPerSecond;

  private final Map<String, LinkedMovingAverageRecord> stats;


  public ServerStatistics() {
    packetsReceived = EndPointServerStatus.SystemTotalPacketsReceived.sum();
    packetsSent = EndPointServerStatus.SystemTotalPacketsSent.sum();

    totalConnections = EndPoint.totalConnections.sum();
    totalReadBytes = EndPoint.totalReadBytes.sum();
    totalWriteBytes = EndPoint.totalWriteBytes.sum();
    totalDisconnections = EndPoint.totalDisconnections.sum();

    publishedPerSecond = DestinationImpl.getGlobalStats().getPublishedPerSecond();
    subscribedPerSecond = DestinationImpl.getGlobalStats().getSubscribedPerSecond();
    noInterestPerSecond = DestinationImpl.getGlobalStats().getNoInterestPerSecond();
    deliveredPerSecond = DestinationImpl.getGlobalStats().getDeliveredPerSecond();
    retrievedPerSecond = DestinationImpl.getGlobalStats().getRetrievedPerSecond();

    totalNoInterestMessages = DestinationImpl.getGlobalStats().getTotalNoInterestMessages();
    totalSubscribedMessages = DestinationImpl.getGlobalStats().getTotalSubscribedMessages();
    totalPublishedMessages = DestinationImpl.getGlobalStats().getTotalPublishedMessages();
    totalRetrievedMessages = DestinationImpl.getGlobalStats().getTotalRetrievedMessages();
    totalDeliveredMessages = DestinationImpl.getGlobalStats().getTotalDeliveredMessages();
    totalExpiredMessages = DestinationImpl.getGlobalStats().getTotalExpiredMessages();
    stats = DestinationImpl.getGlobalStats().getGlobalStats();
  }
}
