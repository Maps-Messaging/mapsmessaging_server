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
import java.util.Objects;

public class N2kJsonListenerRegistry {

  private final Map<Integer, N2kJsonListener> listeners;

  public N2kJsonListenerRegistry() {
    this(
        new N2kPositionJsonListener(),
        new N2kGnssJsonListener(),
        new N2kMotionJsonListener(),
        new N2kHeadingJsonListener(),
        new N2kAttitudeJsonListener(),
        new N2kRateOfTurnJsonListener(),
        new N2kGnssDopsJsonListener(),
        new N2kBatteryStatusJsonListener(),
        new N2kMagneticVariationJsonListener(),
        new N2kWindJsonListener(),
        new N2kEnvironmentalParametersJsonListener(),
        new N2kInverterStatusJsonListener());
  }

  N2kJsonListenerRegistry(N2kJsonListener... listeners) {
    this.listeners = new HashMap<>();
    for (N2kJsonListener listener : listeners) {
      register(listener);
    }
  }

  public N2kJsonListener getListener(int pgn) {
    return listeners.get(pgn);
  }

  public boolean hasListener(int pgn) {
    return listeners.containsKey(pgn);
  }

  void register(N2kJsonListener listener) {
    Objects.requireNonNull(listener, "listener must not be null");
    N2kJsonListener existing = listeners.putIfAbsent(listener.getPgn(), listener);
    if (existing != null) {
      throw new IllegalArgumentException("Duplicate N2K JSON listener for PGN " + listener.getPgn());
    }
  }
}
