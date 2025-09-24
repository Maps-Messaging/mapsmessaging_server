/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.config.analytics;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;

import java.util.Arrays;
import java.util.List;

public class StatisticsConfig extends StatisticsConfigDTO implements Config {

  public StatisticsConfig(ConfigurationProperties config){
    int count = config.getIntProperty("eventCount", 100);
    if(count <= 10) {
      count = 10;
    }
    if(count > 1000000){
      count = 1000000;
    }
    eventCount = count;

    String ignoreString = config.getProperty("ignoreList", "");
    if(ignoreString.isEmpty()){
      ignoreList = List.of();
    }
    else{
      ignoreList = Arrays.asList(ignoreString.split(","));
    }

    String entryString = config.getProperty("keyList", "");
    if(!entryString.isEmpty()){
      keyList = Arrays.asList(entryString.split(","));
    }
    else {
      keyList = List.of();
    }
    statisticName = config.getProperty("defaultAnalyser", "Base");
  }


  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("eventCount", eventCount);
    config.put("ignoreList", ignoreList);
    config.put("keyList", keyList);
    config.put("defaultAnalyser", statisticName);
    return null;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean change = false;
    if(config instanceof StatisticsConfig statisticsConfig){
      if(eventCount != statisticsConfig.getEventCount()){
        eventCount = statisticsConfig.getEventCount();
        change = true;
      }
      if(!ignoreList.equals(statisticsConfig.getIgnoreList())){
        ignoreList = statisticsConfig.getIgnoreList();
        change = true;
      }
      if(keyList.equals(statisticsConfig.getKeyList())){
        keyList = statisticsConfig.getKeyList();
        change = true;
      }
      if(!statisticName.equals(statisticsConfig.getStatisticName())){
        statisticName = statisticsConfig.getStatisticName();
        change = true;
      }
    }
    return change;
  }
}
