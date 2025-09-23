package io.mapsmessaging.analytics.impl;

import com.google.gson.JsonObject;
import io.mapsmessaging.analytics.Analyser;
import io.mapsmessaging.analytics.impl.stats.Statistics;
import io.mapsmessaging.analytics.impl.stats.StatisticsFactory;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.selector.IdentifierResolver;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StatisticalAnalyser implements Analyser {

  private final int eventCount;
  private final String[] ignoreList;
  private final Map<String, Statistics> statistics;
  private final String defaultAnalyser;
  private final List<String> entries;

  private int counter;
  private MessageFormatter formatter;


  public StatisticalAnalyser() {
    formatter = null;
    ignoreList = new String[0];
    eventCount = 0;
    entries = List.of();
    defaultAnalyser = null;
    statistics = new LinkedHashMap<>();
  }

  public StatisticalAnalyser(String defaultName, int eventCount, String[] ignoreList, List<String> entries) {
    this.eventCount = eventCount;
    this.ignoreList = ignoreList;
    this.entries = entries;
    this.defaultAnalyser = defaultName;
    statistics = new LinkedHashMap<>();
  }

  @Override
  public Analyser create(@NotNull ConfigurationProperties configuration) {
    int count = configuration.getIntProperty("eventCount", 100);
    if(count <= 10) {
      count = 10;
    }
    if(count > 1000000){
      count = 1000000;
    }

    String ignoreString = configuration.getProperty("ignoreList", "");
    String[] ignore = ignoreString.split(",");

    String entryString = configuration.getProperty("keyList", "");
    String[] entryArray= new String[0];
    if(!entryString.isEmpty()){
      entryArray = entryString.split(",");
    }
    String analyserName = configuration.getProperty("defaultAnalyser", "Base");
    return new StatisticalAnalyser(analyserName, count, ignore, List.of(entryArray));
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
      boolean ignored = ignoreList != null && ignoreList.length > 0 &&
          Arrays.stream(ignoreList).anyMatch(s -> s.startsWith(key));

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
