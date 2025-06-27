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

package io.mapsmessaging.engine.destination;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DestinationUpdateManager {

  private final List<DestinationManagerListener> destinationManagerListeners;

  public DestinationUpdateManager() {
    destinationManagerListeners = new CopyOnWriteArrayList<>();
  }

  public List<DestinationManagerListener> get() {
    return new ArrayList<>(destinationManagerListeners);
  }

  public void created(DestinationImpl destination) {
    for (DestinationManagerListener listener : destinationManagerListeners) {
      listener.created(destination);
    }
  }

  public void deleted(DestinationImpl destination) {
    for (DestinationManagerListener listener : destinationManagerListeners) {
      listener.deleted(destination);
    }
  }

  public void add(DestinationManagerListener listener) {
    destinationManagerListeners.add(listener);
  }

  public boolean remove(DestinationManagerListener listener) {
    return destinationManagerListeners.remove(listener);
  }

}
