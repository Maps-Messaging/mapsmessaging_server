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

import io.mapsmessaging.config.TwinManagerConfig;
import io.mapsmessaging.dto.rest.config.TwinManagerConfigDTO;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.publisher.TwinJsonPublisher;
import io.mapsmessaging.state.drone.tak.TakTwinObserver;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.Getter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.*;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class StateManagerAgent implements Agent {

  private static final long SCAN_INTERVAL_MILLIS = 1000L;
  private static final long PURGE_INTERVAL_MILLIS = 30000L;
  private final Logger logger = LoggerFactory.getLogger(StateManagerAgent.class);

  @Getter
  private final TwinManager twinManager;
  private TakTwinObserver takManager;
  private TwinJsonPublisher twinJsonPublisher;
  private final  TwinManagerConfigDTO config;
  private ScheduledExecutorService scheduler;

  public StateManagerAgent() {
    config = ConfigurationManager.getInstance().getConfiguration(TwinManagerConfig.class);
    if(config == null){
      twinManager = new TwinManager();
    }
    else {
      twinManager = new TwinManager(config.isRemoveExpiredTwins(), config.getStaleTimeoutMillis(), config.getHeartbeatTimeoutMillis(), config.getRetentionTimeoutMillis());
    }
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
    try {
      logger.log(STATE_MANAGER_START);
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


      if (config != null) {
        if (config.getTak() != null) {
          takManager = new TakTwinObserver(twinManager);
          logger.log(STATE_MANAGER_TAK_ENABLED);
        }
        if (config.getPublish() != null) {
          try {
            twinJsonPublisher = new TwinJsonPublisher(twinManager, config.getPublish().getTopicTemplate());
            logger.log(STATE_MANAGER_PUBLISH_ENABLED, config.getPublish().getTopicTemplate());
          } catch (Throwable e) {
            logger.log(STATE_MANAGER_PUBLISH_FAILED, e);
          }
        }
      } else {
        takManager = null;
        twinJsonPublisher = null;
      }
    } finally {
      logger.log(STATE_MANAGER_STARTED);
    }
  }

  @Override
  public synchronized void stop() {
    logger.log(STATE_MANAGER_STOP);
    try {
      if (scheduler == null) {
        return;
      }

      scheduler.shutdownNow();
      scheduler = null;
      if(takManager != null) {
        takManager.shutdown();
      }
      if(twinJsonPublisher != null) {
        try {
          twinJsonPublisher.close();
        } catch (IOException e) {
          logger.log(STATE_MANAGER_PUBLISH_FAILED, e);
        }
      }
    } finally {
      logger.log(STATE_MANAGER_STOPPED);
    }
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