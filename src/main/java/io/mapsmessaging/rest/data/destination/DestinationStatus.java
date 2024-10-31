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

package io.mapsmessaging.rest.data.destination;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStats;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DestinationStatus implements Serializable {

  @Schema(description = "Name of the destination")
  private String name;
  @Schema(description = "Number of stored messages")
  private long storedMessages;
  @Schema(description = "Number of time pending messages")
  private long delayedMessages;
  @Schema(description = "Number of pending transactions messages")
  private long pendingTransactions;
  @Schema(description = "Number of dropped messages")
  private long noInterestMessages;
  @Schema(description = "Number of ppublished messages")
  private long publishedMessages;
  @Schema(description = "Number of retrieved messages")
  private long retrievedMessages;
  @Schema(description = "Number of expired messages")
  private long expiredMessages;
  @Schema(description = "Number of delivered messages")
  private long deliveredMessages;
  @Schema(description = "Average time to read from store")
  private long readTimeAve_ns;
  @Schema(description = "Average time to write to store")
  private long writeTimeAve_ns;
  @Schema(description = "Average time to delete from store")
  private long deleteTimeAve_ns;

  public DestinationStatus(DestinationImpl destinationImpl) {
    this.name = destinationImpl.getFullyQualifiedNamespace();
    storedMessages = getStored(destinationImpl);
    delayedMessages = destinationImpl.getDelayedMessages();
    pendingTransactions = destinationImpl.getPendingTransactions();

    DestinationStats stats = destinationImpl.getStats();
    noInterestMessages = stats.getNoInterest();
    publishedMessages = stats.getMessagePublished();
    retrievedMessages = stats.getRetrievedMessage();
    expiredMessages = stats.getExpiredMessage();
    deliveredMessages = stats.getDeliveredMessages();
    readTimeAve_ns = stats.getMessageReadTime();
    writeTimeAve_ns = stats.getMessageWriteTime();
    deleteTimeAve_ns = stats.getMessageRemovedTime();
  }

  private long getStored(DestinationImpl destination) {
    try {
      return destination.getStoredMessages();
    } catch (IOException e) {
    }
    return 0;
  }
}