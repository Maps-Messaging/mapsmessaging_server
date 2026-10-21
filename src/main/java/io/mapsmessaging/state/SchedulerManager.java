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

package io.mapsmessaging.state;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.utilities.Lifecycle;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.state.logging.StateLogMessages.STATE_MANAGER_SCHEDULER_ERROR;

public class SchedulerManager implements Lifecycle {
  private static final long SCAN_INTERVAL_MILLIS = 1000L;
  private static final long PURGE_INTERVAL_MILLIS = 30000L;

  private final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);
  private final TwinManager twinManager;
  private ScheduledExecutorService scheduler;



  public SchedulerManager(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void start() {
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
        logger.log(STATE_MANAGER_SCHEDULER_ERROR, e);
      }
    }, SCAN_INTERVAL_MILLIS, SCAN_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

    scheduler.scheduleAtFixedRate(() -> {
      try {
        twinManager.purgeExpiredTwins(Instant.now());
      } catch (Exception e) {
        logger.log(STATE_MANAGER_SCHEDULER_ERROR, e);
      }
    }, PURGE_INTERVAL_MILLIS, PURGE_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

  }

  @Override
  public void stop() {
    if (scheduler == null) {
      return;
    }
    scheduler.shutdownNow();
    scheduler = null;
  }
}
