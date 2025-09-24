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

package io.mapsmessaging.analytics.impl;

import com.google.gson.JsonObject;
import io.mapsmessaging.analytics.Analyser;
import io.mapsmessaging.analytics.impl.stats.Statistics;
import io.mapsmessaging.analytics.impl.stats.StatisticsFactory;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.selector.IdentifierResolver;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StatisticalAnalyser implements Analyser {

  private final int eventCount;
  private final List<String> ignoreList;
  private final Map<String, Statistics> statistics;
  private final String defaultAnalyser;
  private final List<String> entries;

  private int counter;
  private MessageFormatter formatter;


  public StatisticalAnalyser() {
    formatter = null;
    ignoreList = List.of();
    eventCount = 0;
    entries = List.of();
    defaultAnalyser = null;
    statistics = new LinkedHashMap<>();
  }

  public StatisticalAnalyser(String defaultName, int eventCount, List<String> ignoreList, List<String> entries) {
    this.eventCount = eventCount;
    this.ignoreList = ignoreList;
    this.entries = entries;
    this.defaultAnalyser = defaultName;
    statistics = new LinkedHashMap<>();
  }

  @Override
  public Analyser create(@NotNull StatisticsConfigDTO config) {
    return new StatisticalAnalyser(config.getStatisticName(), config.getEventCount(), config.getIgnoreList(),config.getKeyList());
  }

  @Override
  public Message ingest(@NotNull Message event) {
    if(formatter == null) {
      String schemaId = event.getSchemaId();
      formatter = SchemaManager.getInstance().getMessageFormatter(schemaId);
    }
    if(formatter != null) {
      IdentifierResolver resolver = formatter.parse(event.getOpaqueData());
      if(statistics.isEmpty()) {
        loadEntries(resolver);
      }
      for(Map.Entry<String, Statistics> entry : statistics.entrySet()) {
        Object val = resolver.get(entry.getKey());
        if(val != null) {
          entry.getValue().update(val);
        }
        else{
          entry.getValue().incrementMismatch();
        }
      }

      counter++;
      if(counter >= eventCount) {
        return buildEvent();
      }
      return null;
    }
    return event;
  }

  private Message buildEvent() {
    counter = 0;
    JsonObject o = new JsonObject();
    for(Map.Entry<String, Statistics> entry : statistics.entrySet()) {
      o.add(entry.getKey(), entry.getValue().toJson());
      entry.getValue().reset();
    }
    String jsonString = o.toString();

    MessageBuilder mb = new MessageBuilder();
    mb.setOpaqueData(jsonString.getBytes());
    mb.setContentType("application/json");
    return  mb.build();
  }

  private void loadEntries(IdentifierResolver resolver) {
    resolver.getKeys().forEach(key -> {
      boolean ignored = ignoreList != null && !ignoreList.isEmpty() &&
          ignoreList.stream().anyMatch(s -> s.startsWith(key));

      if (!ignored) {
        boolean inEntries = entries.isEmpty() ||
            entries.stream().anyMatch(s -> s.startsWith(key));

        if (inEntries) {
          Object value = resolver.get(key);
          if (value instanceof String) {
            statistics.put(key, StatisticsFactory.getInstance().getAnalyser("String"));
          } else if (value instanceof Number) {
            statistics.put(key, StatisticsFactory.getInstance().getAnalyser(defaultAnalyser));
          }
        }
      }
    });
  }

  @Override
  public Message flush() {
    return buildEvent();
  }

  @Override
  public void close() {
    //nothing to close here
  }

  @Override
  public String getName() {
    return "stats";
  }

  @Override
  public String getDescription() {
    return "Statistical Event Analyser";
  }
}
