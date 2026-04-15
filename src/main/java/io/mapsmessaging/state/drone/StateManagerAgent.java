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

package io.mapsmessaging.state.drone;

import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.utilities.Agent;
import lombok.Getter;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class StateManagerAgent implements Agent {

  private static final long SCAN_INTERVAL_MILLIS = 1000L;
  private static final long PURGE_INTERVAL_MILLIS = 30000L;

  @Getter
  private final TwinManager twinManager;

  private ScheduledExecutorService scheduler;

  public StateManagerAgent() {
    twinManager = new TwinManager();
  }

  @Override
  public String getName() {
    return "State Manager";
  }

  @Override
  public String getDescription() {
    return "Manages state of known objects within memory and maintains a digital twin";
  }

  @Override
  public synchronized void start() {
    if (scheduler != null && !scheduler.isShutdown()) {
      return;
    }

    scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "StateManagerAgent");
      thread.setDaemon(true);
      return thread;
    });

    scheduler.scheduleAtFixedRate(() -> {
      try {
        twinManager.scanTwinStates(Instant.now());
      } catch (Exception e) {
        // swallow to keep scheduler alive
      }
    }, SCAN_INTERVAL_MILLIS, SCAN_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

    scheduler.scheduleAtFixedRate(() -> {
      try {
        twinManager.purgeExpiredTwins(Instant.now());
      } catch (Exception e) {
        // swallow to keep scheduler alive
      }
    }, PURGE_INTERVAL_MILLIS, PURGE_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
  }

  @Override
  public synchronized void stop() {
    if (scheduler == null) {
      return;
    }

    scheduler.shutdownNow();
    scheduler = null;
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment(
        "Current objects: " + twinManager.getTwinCount()
            + ", active: " + twinManager.getTwinCountByStatus(TwinLifecycleStatus.ACTIVE)
            + ", disconnected: " + twinManager.getTwinCountByStatus(TwinLifecycleStatus.DISCONNECTED)
            + ", stale: " + twinManager.getTwinCountByStatus(TwinLifecycleStatus.STALE)
    );
    status.setStatus(Status.OK);
    return status;
  }
}