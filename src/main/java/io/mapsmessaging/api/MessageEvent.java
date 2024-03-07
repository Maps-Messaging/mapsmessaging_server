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

package io.mapsmessaging.api;

import io.mapsmessaging.api.message.Message;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

@Data
public class MessageEvent {

  private final String destinationName;
  private final SubscribedEventManager subscription;
  private final Message message;
  private final Runnable completionTask;

  public MessageEvent(
      @NonNull
      @NotNull
      String destinationName,
      @NonNull
      @NotNull
      SubscribedEventManager subscription,
      @NonNull
      @NotNull
      Message message,
      @NonNull
      @NotNull
      Runnable completionTask) {
    this.destinationName = destinationName;
    this.subscription = subscription;
    this.message = message;
    this.completionTask = completionTask;
  }
}
