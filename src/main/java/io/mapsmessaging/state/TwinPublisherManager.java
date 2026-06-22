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
import io.mapsmessaging.state.config.TwinPublishConfigDTO;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.publisher.TwinJsonPublisher;
import io.mapsmessaging.utilities.Lifecycle;

import java.io.IOException;

import static io.mapsmessaging.logging.ServerLogMessages.STATE_MANAGER_PUBLISH_ENABLED;
import static io.mapsmessaging.logging.ServerLogMessages.STATE_MANAGER_PUBLISH_FAILED;

public class TwinPublisherManager implements Lifecycle {

  private final Logger logger = LoggerFactory.getLogger(TwinPublisherManager.class);

  private final TwinPublishConfigDTO config;
  private TwinJsonPublisher twinJsonPublisher;
  private final TwinManager twinManager;

  public TwinPublisherManager (TwinManager twinManager, TwinPublishConfigDTO config){
    this.config = config;
    this.twinManager = twinManager;
  }

  @Override
  public void start() {
    if(config != null && config.isEnabled()){
      try {
        twinJsonPublisher = new TwinJsonPublisher(twinManager, config.getTopicTemplate());
        logger.log(STATE_MANAGER_PUBLISH_ENABLED, config.getTopicTemplate());
      } catch (Throwable e) {
        logger.log(STATE_MANAGER_PUBLISH_FAILED, e);
      }
    }
  }

  @Override
  public void stop() {
    if(twinJsonPublisher != null) {
      try {
        twinJsonPublisher.close();
      } catch (IOException e) {
        logger.log(STATE_MANAGER_PUBLISH_FAILED, e);
      }
    }
  }
}
