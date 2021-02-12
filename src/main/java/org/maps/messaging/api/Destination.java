/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.api;

import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.BaseDestination;
import org.maps.messaging.engine.destination.DestinationImpl;

/**
 * Generic destination class 
 */
public class Destination implements BaseDestination {

  protected final DestinationImpl destinationImpl;

  Destination(@NotNull DestinationImpl impl) {
    destinationImpl = impl;
  }

  public int storeMessage(@NotNull Message message) throws IOException {
    return destinationImpl.storeMessage(message);
  }

  public String getName() {
    return destinationImpl.getName();
  }

  public long getStoredMessages() throws IOException {
    return destinationImpl.getStoredMessages();
  }

  public DestinationType getResourceType() {
    return destinationImpl.getResourceType();
  }
}
