/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.dto.rest.session;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubscriptionStateDTO {

  private String destinationName;
  private int size;
  private int pending;

  private boolean hasMessagesInFlight;
  private boolean hasAtRestMessages;

  private boolean isPaused;

  private long messagesIgnored;
  private long messagesRegistered;
  private long messagesSent;
  private long messagesAcked;
  private long messagesRolledBack;
  private long messagesExpired;
}
