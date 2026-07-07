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

import java.util.HashMap;
import java.util.Map;

public class N2kJsonListenerRegistry {

  private final Map<Integer, N2kJsonListener> listeners;

  public N2kJsonListenerRegistry() {
    listeners = new HashMap<>();

    register(new N2kPositionJsonListener());
    register(new N2kGnssJsonListener());
    register(new N2kMotionJsonListener());
    register(new N2kHeadingJsonListener());
    register(new N2kAttitudeJsonListener());

    register(new N2kRateOfTurnJsonListener());
    register(new N2kGnssDopsJsonListener());
    register(new N2kBatteryStatusJsonListener());
    register(new N2kMagneticVariationJsonListener());
    register(new N2kWindJsonListener());
    register(new N2kEnvironmentalParametersJsonListener());
    register(new N2kInverterStatusJsonListener());
  }

  public N2kJsonListener getListener(int pgn) {
    return listeners.get(pgn);
  }

  public boolean hasListener(int pgn) {
    return listeners.containsKey(pgn);
  }

  private void register(N2kJsonListener listener) {
    listeners.put(listener.getPgn(), listener);
  }
}