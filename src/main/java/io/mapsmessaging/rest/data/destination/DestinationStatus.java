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

package io.mapsmessaging.rest.data.destination;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.utilities.stats.LinkedMovingAverageRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

@Data
public class DestinationStatus implements Serializable {

  @Schema(description = "Name of the destination")
  private final String name;

  @Schema(description = "Number of stored messages")
  private final long storedMessages;
  @Schema(description = "Number of time pending messages")
  private final long delayedMessages;
  @Schema(description = "Number of pending transactions messages")
  private final long pendingTransactions;
  @Schema(description = "Number of dropped messages")
  private final long noInterestMessages;
  @Schema(description = "Number of ppublished messages")
  private final long publishedMessages;
  @Schema(description = "Number of retrieved messages")
  private final long retrievedMessages;
  @Schema(description = "Number of expired messages")
  private final long expiredMessages;
  @Schema(description = "Number of delivered messages")
  private final long deliveredMessages;
  @Schema(description = "Average time to read from store")
  private final long readTimeAve_ns;
  @Schema(description = "Average time to write to store")
  private final long writeTimeAve_ns;
  @Schema(description = "Average time to delete from store")
  private final long deleteTimeAve_ns;

  /*
    @Schema(description = "Map of moving averages")
    private final Map<String, LinkedMovingAverageRecord> statistics;
    @Schema(description = "Map of storage statistics")
    private final Map<String, Map<String, LinkedMovingAverageRecord>> storeageStatistics;
  */
  public DestinationStatus(DestinationImpl destinationImpl) {
    this.name = destinationImpl.getFullyQualifiedNamespace();
    storedMessages = getStored(destinationImpl);
    delayedMessages = destinationImpl.getDelayedMessages();
    pendingTransactions = destinationImpl.getPendingTransactions();
    DestinationStats stats = destinationImpl.getStats();
    noInterestMessages = stats.getNoInterestMessageAverages().getCurrent();
    publishedMessages = stats.getPublishedMessageAverages().getCurrent();
    retrievedMessages = stats.getRetrievedMessagesAverages().getCurrent();
    expiredMessages = stats.getExpiredMessagesAverages().getCurrent();
    deliveredMessages = stats.getDeliveredMessagesAverages().getCurrent();
    readTimeAve_ns = stats.getReadTimeAverages().getCurrent();
    writeTimeAve_ns = stats.getWriteTimeAverages().getCurrent();
    deleteTimeAve_ns = stats.getDeleteTimeAverages().getCurrent();
/*
    statistics = destinationImpl.getStats().getStatistics();
    if(destinationImpl.getResourceStatistics() != null) {
      storeageStatistics = destinationImpl.getResourceStatistics().getStatistics();
    }
    else{
      storeageStatistics = null;
    }

 */
  }

  private long getStored(DestinationImpl destination) {
    try {
      return destination.getStoredMessages();
    } catch (IOException e) {
    }
    return 0;
  }
}