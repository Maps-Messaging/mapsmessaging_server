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

package io.mapsmessaging.aggregator;

import io.mapsmessaging.config.AggregatorManagerConfig;
import io.mapsmessaging.dto.rest.config.AggregatorManagerConfigDTO;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

public class AggregatorManager implements Agent {

  private AggregatorManagerConfigDTO config;


  public AggregatorManager(){}

  @Override
  public String getName() {
    return "AggregatorManager";
  }

  @Override
  public String getDescription() {
    return "Manages the aggregation of messages from multiple destinations";
  }

  @Override
  public void start() {
    config = ConfigurationManager.getInstance().getConfiguration(AggregatorManagerConfig.class);
  }

  @Override
  public void stop() {

  }

  @Override
  public SubSystemStatusDTO getStatus() {
    return null;
  }
}
