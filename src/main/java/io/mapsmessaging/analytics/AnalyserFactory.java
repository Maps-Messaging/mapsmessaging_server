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

package io.mapsmessaging.analytics;

import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class AnalyserFactory {

  private static class Holder {
    static final AnalyserFactory INSTANCE = new AnalyserFactory();
  }

  public static AnalyserFactory getInstance() {
    return AnalyserFactory.Holder.INSTANCE;
  }


  private final Map<String, Analyser> analysers;

  private AnalyserFactory() {
    analysers = new ConcurrentHashMap<>();
    ServiceLoader<Analyser> serviceLoaded = ServiceLoader.load(Analyser.class);
    for(Analyser analyser : serviceLoaded) {
      analysers.put(analyser.getName(), analyser);
    }
  }

  public Analyser getAnalyser(String analyserName, ConfigurationProperties properties) {
    Analyser analyser = analysers.get(analyserName);
    if(analyser != null) {
      return analyser.create(properties);
    }
    return null;
  }

}
