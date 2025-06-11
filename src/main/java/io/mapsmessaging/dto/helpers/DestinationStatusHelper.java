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

import io.mapsmessaging.dto.rest.destination.DestinationDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStats;

import java.io.IOException;

public class DestinationStatusHelper {

  public static DestinationDTO createDestination(DestinationImpl destinationImpl) {
    if(destinationImpl == null) {
      return null;
    }
    DestinationStats stats = destinationImpl.getStats();

    return new DestinationDTO(
        destinationImpl.getFullyQualifiedNamespace(),
        destinationImpl.getResourceType().getName(),
        getStored(destinationImpl),
        destinationImpl.getDelayedMessages(),
        destinationImpl.getPendingTransactions(),
        destinationImpl.getSchema().getUniqueId(),
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

  private static long getStored(DestinationImpl destinationImpl) {
    try {
      return destinationImpl.getStoredMessages();
    } catch (IOException e) {
      // todo log this
    }
    return 0;
  }

  private DestinationStatusHelper() {
  }
}
