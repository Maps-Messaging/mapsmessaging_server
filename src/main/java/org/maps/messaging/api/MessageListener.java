/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.maps.messaging.api;

import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.message.Message;

/**
 * This interface needs to be implemented and passed in for each Session to enable any messages that
 * match a subscription to be delivered to the Session.
 *
 * Please note: This may mean multiple simultaneous threads hit this function and any implementation will
 * need to handle the threading.
 *
 */
public interface MessageListener {

  /**
   *
   * @param destination The Destination that the message was from
   * @param subscription The subscribedEventManager to use to acknowledge and integrate path of the message
   * @param message The message that was sent
   * @param completionTask This task needs to be called once all delivery action is done
   */
  void sendMessage( @NotNull Destination destination,
                    @NotNull String normalisedName,
                    @NotNull SubscribedEventManager subscription,
                    @NotNull Message message,
                    @NotNull Runnable completionTask);

}
