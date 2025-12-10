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

package io.mapsmessaging.analytics.impl.stats;


import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import static io.mapsmessaging.logging.ServerLogMessages.STATISTICS_UNKNOWN_NAME;

public class StatisticsFactory {


  private static class Holder {
    static final StatisticsFactory INSTANCE = new StatisticsFactory();
  }

  public static StatisticsFactory getInstance() {
    return StatisticsFactory.Holder.INSTANCE;
  }


  private final Logger logger = LoggerFactory.getLogger(StatisticsFactory.class);

  private final Map<String, Statistics> statistics;

  private StatisticsFactory() {
    statistics = new ConcurrentHashMap<>();
    ServiceLoader<Statistics> serviceLoaded = ServiceLoader.load(Statistics.class);
    for(Statistics statistic : serviceLoaded) {
      statistics.put(statistic.getName(), statistic);
    }
  }

  public Statistics getAnalyser(String statisticsName) {
    Statistics statistic = statistics.get(statisticsName);
    if(statistic != null) {
      return statistic.create();
    }
    logger.log(STATISTICS_UNKNOWN_NAME, statisticsName, "Base");
    return statistics.get("Base");
  }
}
