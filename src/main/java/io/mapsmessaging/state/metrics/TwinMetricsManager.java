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

package io.mapsmessaging.state.metrics;

import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.utilities.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/** Registers one {@link TwinStatusJMX} per twin type and lifecycle status. */
public class TwinMetricsManager implements Lifecycle {

  private final TwinManager twinManager;
  private final List<TwinStatusJMX> beans = new ArrayList<>();

  public TwinMetricsManager(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public synchronized void start() {
    for (TwinType twinType : TwinType.values()) {
      for (TwinLifecycleStatus status : TwinLifecycleStatus.values()) {
        beans.add(new TwinStatusJMX(twinManager, twinType, status));
      }
    }
  }

  @Override
  public synchronized void stop() {
    for (TwinStatusJMX bean : beans) {
      bean.close();
    }
    beans.clear();
  }
}
