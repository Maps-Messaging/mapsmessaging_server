/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.util.Set;

public class N2kJsonDispatcher {

  private static final Set<Integer> IGNORED_PGNS = Set.of(
      N2kPgns.ISO_REQUEST,
      N2kPgns.ISO_ADDRESS_CLAIM,
      N2kPgns.SYSTEM_TIME,
      N2kPgns.TIME_DATE,
      N2kPgns.HEARTBEAT
  );

  private final N2kJsonListenerRegistry listeners;

  public N2kJsonDispatcher(N2kJsonListenerRegistry listeners) {
    this.listeners = listeners;
  }

  public void dispatch(DroneTwin droneTwin, int pgn, JsonObject packet, TwinUpdateContext context) {
    N2kJsonListener listener = listeners.getListener(pgn);
    if (listener == null) {
      if (!IGNORED_PGNS.contains(pgn)) {
        System.err.println("No listener for " + pgn + " with " + packet);
      }
      return;
    }

    listener.handle(droneTwin, packet, context);
  }
}