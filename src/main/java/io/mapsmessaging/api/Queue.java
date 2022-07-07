/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.api;

import io.mapsmessaging.engine.destination.DestinationImpl;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * Simple wrapper class that indicates that the destination is of type Queue. This means that the reads are destructive and messages can only be delivered once to a single
 * destination.
 */
public class Queue extends Destination {

  Queue(@NonNull @NotNull DestinationImpl impl) {
    super(impl);
  }

}
