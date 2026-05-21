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

import io.mapsmessaging.aggregator.worker.AggregatorWorkScheduler;
import io.mapsmessaging.config.AggregatorManagerConfig;
import io.mapsmessaging.dto.rest.config.AggregatorManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class AggregatorManager implements Agent {

  private final List<Aggregator> aggregators;
  private final Logger logger = LoggerFactory.getLogger(AggregatorManager.class);
  private final AggregatorManagerConfigDTO config;
  private AggregatorWorkScheduler aggregatorWorkScheduler;

  public AggregatorManager(){
    config = ConfigurationManager.getInstance().getConfiguration(AggregatorManagerConfig.class);
    aggregators = new ArrayList<>();
    if(config != null && config.getAggregatorConfigList() != null && !config.getAggregatorConfigList().isEmpty()) {
      aggregatorWorkScheduler = new AggregatorWorkScheduler(
          config.getStripeCount(),
          config.getMaxBatchPerAggregator(),
          config.getIdleSleepMs()
      );
      for (AggregatorConfigDTO aggregatorConfig : config.getAggregatorConfigList()) {
        if(containsWildcard(aggregatorConfig)){
          aggregators.add(new DynamicAggregatorManager(aggregatorWorkScheduler, aggregatorConfig));
        }
        else {
          aggregators.add(new StaticAggregator(aggregatorWorkScheduler, aggregatorConfig));
        }
      }
      logger.log(AGGREGATOR_MANAGER_TASK_CREATED, config.getAggregatorConfigList().size());
    }
    else{
      logger.log(AGGREGATOR_MANAGER_TASK_CREATED);
    }
  }

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
    if(aggregatorWorkScheduler != null) {
      aggregatorWorkScheduler.start();
      for (Aggregator staticAggregator : aggregators) {
        staticAggregator.start();
      }
      logger.log(AGGREGATOR_MANAGER_TASK_STARTED, aggregators.size());
    }
  }

  @Override
  public void stop() {
    if(aggregatorWorkScheduler != null) {
      aggregatorWorkScheduler.stop();
      for (Aggregator staticAggregator : aggregators) {
        staticAggregator.stop();
      }
      logger.log(AGGREGATOR_MANAGER_TASK_STOPPED, aggregators.size());
    }
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    if(config == null || config.getAggregatorConfigList() == null || config.getAggregatorConfigList().isEmpty()) {
      status.setStatus(Status.DISABLED);
      status.setComment("No configured aggregators");
    }
    else{
      status.setStatus(Status.OK);
      status.setComment("Running Aggregators:"+config.getAggregatorConfigList().size());
    }
    return status;
  }

  private boolean containsWildcard(AggregatorConfigDTO config) {
    for (AggregatorInputConfigDTO input : config.getInputs()) {
      String topic = input.getTopicName();
      if (topic.contains("+") || topic.contains("#")) {
        return true;
      }
    }
    return false;
  }
}
