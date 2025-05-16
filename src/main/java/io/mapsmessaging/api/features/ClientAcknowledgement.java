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

package io.mapsmessaging.api.features;

import lombok.Getter;

/**
 * Enum that supports the different modes of message acknowledgement that is supported
 */
public enum ClientAcknowledgement {
  /**
   * All message deliveries are automatically acknowledged once the completion task is called on the MessageListener
   */
  AUTO(
      0,
      "Server side acknowledgement"
  ),
  /**
   * Each message MUST be manually acknowledged by calling the SubscribedEventManager.ackReceived or SubscribedEventManager.rollbackReceived. Once the number of in-flight messages
   * is exceeded the messaging engine will stop delivering messages.
   */
  INDIVIDUAL(
      1,
      "Individual messages need to be acknowledged"
  ),
  /**
   * This is similar to INDIVIDUAL, except, that ALL messages from the MessageId, supplied in the ackReceived or the rollbackReceived, to the lowest Message ID will be processed.
   * Say you have 10 events in-flight such as 1, 2 ,3 ,4 ,5 ,6 ,7 ,8 ,9 and 10. A call to ackReceived(5) is made then messages 1,2,3,4 and 5 will be acknowledged rather than just
   * 5.
   */
  BLOCK(
      2,
      "All messages up to an known point are acknowledged"
  );

  @Getter
  private final int value;
  @Getter
  private final String description;

  ClientAcknowledgement(int value, String description) {
    this.value = value;
    this.description = description;
  }

  public static ClientAcknowledgement getInstance(int clientAcknowledgementValue) {
    switch (clientAcknowledgementValue) {
      case 0:
        return AUTO;

      case 1:
        return INDIVIDUAL;

      case 2:
        return BLOCK;

      default:
        throw new IllegalArgumentException("Invalid handler value supplied:" + clientAcknowledgementValue);
    }
  }
}
