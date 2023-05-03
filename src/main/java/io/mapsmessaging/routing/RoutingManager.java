/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.routing;

import static io.mapsmessaging.logging.ServerLogMessages.ROUTING_SHUTDOWN;
import static io.mapsmessaging.logging.ServerLogMessages.ROUTING_STARTUP;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;

public class RoutingManager implements Agent {

  private final Logger logger = LoggerFactory.getLogger(RoutingManager.class);

  private final ConfigurationProperties properties;
  private final boolean enabled;

  public RoutingManager() {
    properties = ConfigurationManager.getInstance().getProperties("routing");
    enabled = properties.getProperty("enable", "false").equalsIgnoreCase("true");
  }

  @Override
  public String getName() {
    return "Event Routing Manager";
  }

  @Override
  public String getDescription() {
    return "Monitors remote server status and manages event routing rules for this server";
  }

  public void start() {
    if (enabled) {
      logger.log(ROUTING_STARTUP);
    }
  }

  public void stop() {
    logger.log(ROUTING_SHUTDOWN);
  }

}