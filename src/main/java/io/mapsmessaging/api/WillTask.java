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

package io.mapsmessaging.api;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.session.will.WillTaskImpl;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * THis class manages the updating of the last Will task. The Will task is executed if the client is dosconnected rather than closes a session. This enables a message to be sent to
 * notify others of a disruption to message flow
 */
public class WillTask {

  private final WillTaskImpl willTaskImpl;

  WillTask(WillTaskImpl impl) {
    willTaskImpl = impl;
  }

  /**
   * Update the data to send when the session is disconnected
   *
   * @param payload to send
   */
  public void updateMessage(@NonNull byte[] payload) {
    willTaskImpl.updateMessage(payload);
  }

  /**
   * Cancel and close the will task so it is removed from the session disconnected
   */
  public void cancel() {
    willTaskImpl.cancel();
  }

  /**
   * Updates the Quality Of Service for the message to be sent
   *
   * @param qos QualityOfService to be used
   * @see QualityOfService
   */
  public void updateQoS(@NonNull @NotNull QualityOfService qos) {
    willTaskImpl.updateQoS(qos);
  }

  public void updateRetainFlag(boolean flag) {
    willTaskImpl.updateRetain(flag);
  }

  /**
   * Updates the destination to send the message to
   *
   * @param destination name of a valid destination to send the message to
   */
  public void updateTopic(@NonNull @NotNull String destination) {
    willTaskImpl.updateTopic(destination);
  }
}
