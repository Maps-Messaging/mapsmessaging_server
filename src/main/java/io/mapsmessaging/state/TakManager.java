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
import io.mapsmessaging.utilities.Lifecycle;

import static io.mapsmessaging.logging.ServerLogMessages.STATE_MANAGER_TAK_ENABLED;

public class TakManager implements Lifecycle {
  private final Logger logger = LoggerFactory.getLogger(TakManager.class);


  private final TakProtocolDTO tak;

  private TakTwinObserver takTwinObserver;
  private final TwinManager twinManager;

  public TakManager(TwinManager twinManager, TakProtocolDTO tak) {
    this.tak = tak;
    this.twinManager = twinManager;
  }

  @Override
  public void start() {
    if (tak != null) {
      takTwinObserver = new TakTwinObserver(twinManager);
      logger.log(STATE_MANAGER_TAK_ENABLED);
    }
    else{
      takTwinObserver = null;
    }
  }

  @Override
  public void stop() {
    if(takTwinObserver != null){
      takTwinObserver.shutdown();
    }
  }
}
