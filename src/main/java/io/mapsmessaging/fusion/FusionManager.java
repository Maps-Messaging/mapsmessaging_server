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

import io.mapsmessaging.aggregator.worker.FusionWorkScheduler;
import io.mapsmessaging.config.FusionManagerConfig;
import io.mapsmessaging.dto.rest.config.FusionManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.FusionConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.FusionInputConfigDTO;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class FusionManager implements Agent {

  private final List<Fusion> aggregators;
  private final Logger logger = LoggerFactory.getLogger(FusionManager.class);
  private final FusionManagerConfigDTO config;
  private FusionWorkScheduler aggregatorWorkScheduler;

  public FusionManager(){
    config = ConfigurationManager.getInstance().getConfiguration(FusionManagerConfig.class);
    aggregators = new ArrayList<>();
    if(config != null && config.getAggregatorConfigList() != null && !config.getAggregatorConfigList().isEmpty()) {
      aggregatorWorkScheduler = new FusionWorkScheduler(
          config.getStripeCount(),
          config.getMaxBatchPerAggregator(),
          config.getIdleSleepMs()
      );
      for (FusionConfigDTO aggregatorConfig : config.getAggregatorConfigList()) {
        if(containsWildcard(aggregatorConfig)){
          aggregators.add(new DynamicFusionManager(aggregatorWorkScheduler, aggregatorConfig));
        }
        else {
          aggregators.add(new StaticFusion(aggregatorWorkScheduler, aggregatorConfig));
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
    return "FusionManager";
  }

  @Override
  public String getDescription() {
    return "Manages the aggregation of messages from multiple destinations";
  }

  @Override
  public void start() {
    if(aggregatorWorkScheduler != null) {
      aggregatorWorkScheduler.start();
      for (Fusion staticFusion : aggregators) {
        staticFusion.start();
      }
      logger.log(AGGREGATOR_MANAGER_TASK_STARTED, aggregators.size());
    }
  }

  @Override
  public void stop() {
    if(aggregatorWorkScheduler != null) {
      aggregatorWorkScheduler.stop();
      for (Fusion staticFusion : aggregators) {
        staticFusion.stop();
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
      status.setComment("Running Fusions:"+config.getAggregatorConfigList().size());
    }
    return status;
  }

  private boolean containsWildcard(FusionConfigDTO config) {
    for (FusionInputConfigDTO input : config.getInputs()) {
      String topic = input.getTopicName();
      if (topic.contains("+") || topic.contains("#")) {
        return true;
      }
    }
    return false;
  }
}
