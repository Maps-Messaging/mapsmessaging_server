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

import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.tak.TakTwinObserver;
import io.mapsmessaging.state.drone.tak.CotIdentityRegistry;
import io.mapsmessaging.state.drone.tak.CotStateSubscriber;
import io.mapsmessaging.state.drone.tak.CotTwinUpdater;
import io.mapsmessaging.state.drone.tak.CotTaskProfile;
import io.mapsmessaging.state.drone.tak.CotTaskPublisher;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.config.TwinManagerConfigDTO;
import io.mapsmessaging.state.config.cot.CotTwinConfigDTO;
import io.mapsmessaging.state.task.CanonicalTaskRegistry;
import io.mapsmessaging.utilities.Lifecycle;

import static io.mapsmessaging.state.logging.StateLogMessages.STATE_MANAGER_TAK_ENABLED;

public class TakManager implements Lifecycle {
  private final Logger logger = LoggerFactory.getLogger(TakManager.class);


  private final TakProtocolDTO tak;

  private TakTwinObserver takTwinObserver;
  private final TwinManager twinManager;
  private final CotTwinConfigDTO cotMapping;
  private final DroneInfoRegistry droneInfoRegistry;
  private CotStateSubscriber cotStateSubscriber;
  private final CanonicalTaskRegistry taskRegistry;
  private CotTaskPublisher cotTaskPublisher;

  public TakManager(TwinManager twinManager, TakProtocolDTO tak) {
    this.tak = tak;
    this.twinManager = twinManager;
    this.cotMapping = null;
    this.droneInfoRegistry = null;
    this.taskRegistry = new CanonicalTaskRegistry();
  }

  public TakManager(
      TwinManager twinManager,
      TakProtocolDTO tak,
      CotTwinConfigDTO cotMapping,
      DroneInfoRegistry droneInfoRegistry,
      CanonicalTaskRegistry taskRegistry) {
    this.tak = tak;
    this.twinManager = twinManager;
    this.cotMapping = cotMapping;
    this.droneInfoRegistry = droneInfoRegistry;
    this.taskRegistry = taskRegistry;
  }

  @Override
  public void start() {
    if (tak != null || (cotMapping != null && cotMapping.isEnabled())) {
      CotTwinConfigDTO activeCotMapping =
          cotMapping != null && cotMapping.isEnabled() ? cotMapping : null;
      takTwinObserver = new TakTwinObserver(twinManager, activeCotMapping);
      logger.log(STATE_MANAGER_TAK_ENABLED);
    }
    else{
      takTwinObserver = null;
    }
    if (cotMapping != null && cotMapping.isEnabled()) {
      try {
        CotIdentityRegistry identityRegistry = new CotIdentityRegistry(cotMapping);
        CotTwinUpdater twinUpdater = new CotTwinUpdater(twinManager, droneInfoRegistry, identityRegistry);
        CotTaskProfile taskProfile = new CotTaskProfile(identityRegistry, taskRegistry);
        cotTaskPublisher = new CotTaskPublisher(
            taskRegistry, identityRegistry, taskProfile, cotMapping.getOutboundTopic());
        cotStateSubscriber = new CotStateSubscriber(cotMapping.getInboundTopic(), twinUpdater, taskProfile);
        cotStateSubscriber.start();
      } catch (IOException exception) {
        closeCotResources();
        throw new IllegalStateException("Unable to start CoT state subscriber", exception);
      }
    }
  }

  @Override
  public void stop() {
    if(takTwinObserver != null){
      takTwinObserver.shutdown();
    }
    IOException failure = closeCotResources();
    if (failure != null) {
      throw new IllegalStateException("Unable to stop CoT state integration", failure);
    }
  }

  private IOException closeCotResources() {
    IOException failure = null;
    if (cotStateSubscriber != null) {
      try {
        cotStateSubscriber.stop();
      } catch (IOException exception) {
        failure = exception;
      } finally {
        cotStateSubscriber = null;
      }
    }
    if (cotTaskPublisher != null) {
      try {
        cotTaskPublisher.close();
      } catch (IOException exception) {
        if (failure == null) {
          failure = exception;
        } else {
          failure.addSuppressed(exception);
        }
      } finally {
        cotTaskPublisher = null;
      }
    }
    return failure;
  }
}
