/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging]
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

package io.mapsmessaging.dto.helpers;

import io.mapsmessaging.dto.rest.destination.DestinationStatusDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStats;
import java.io.IOException;

public class DestinationStatusHelper {


  public static DestinationStatusDTO createDestinationStatus(DestinationImpl destinationImpl){
    DestinationStats stats = destinationImpl.getStats();
    return new DestinationStatusDTO(
        destinationImpl.getFullyQualifiedNamespace(),
        getStored(destinationImpl),
        destinationImpl.getDelayedMessages(),
        destinationImpl.getPendingTransactions(),
        stats.getNoInterest(),
        stats.getMessagePublished(),
        stats.getRetrievedMessage(),
        stats.getExpiredMessage(),
        stats.getDeliveredMessages(),
        stats.getMessageReadTime(),
        stats.getMessageWriteTime(),
        stats.getMessageRemovedTime()
    );

  }

  private static long getStored(DestinationImpl destinationImpl){
    try {
      return destinationImpl.getStoredMessages();
    } catch (IOException e) {
    }
    return 0;
  }
}
