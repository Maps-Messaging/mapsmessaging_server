package io.mapsmessaging.network.protocol.impl.nmea.sentences;

import io.mapsmessaging.network.protocol.impl.nmea.types.EnumTypeFactory;
import io.mapsmessaging.network.protocol.impl.nmea.types.Type;
import io.mapsmessaging.network.protocol.impl.nmea.types.TypeFactory;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SentenceParser {

  private final String name;
  private final String description;
  private final List<Config> configs;

  public SentenceParser(String name, ConfigurationProperties config){
    this.name = name;
    this.configs = new ArrayList<>();
    this.description = config.getProperty("description");
    ConfigurationProperties syntax = (ConfigurationProperties) config.get("syntax");
    int idx = 1;
    ConfigurationProperties entry = (ConfigurationProperties) syntax.get(""+idx);
    while(entry != null){
      String valueName = entry.getProperty("name");
      String typeName = entry.getProperty("type");
      String param = entry.getProperty("param", "");
      int repeats = entry.getIntProperty("repeat", 1);

      if(!param.isEmpty() && typeName.equalsIgnoreCase("enum")){
        EnumTypeFactory.getInstance().register(valueName, param);
      }
      this.configs.add(new Config(valueName, typeName, param, repeats));
      idx++;
      entry = (ConfigurationProperties) syntax.get(""+idx);
    }
  }

  public Sentence parse(Iterator<String> entries){
    List<String> order = new ArrayList<>();
    Map<String, Type> values = new LinkedHashMap<>();
    for(Config config:configs){
      int repeat = config.repeats;
      for(int x=0;x<repeat;x++) {
        Type type = TypeFactory.create(config.name, config.type, config.parameters, entries);
        if(type != null) {
          if (repeat == 1) {
            values.put(config.name, type);
            order.add(config.name);
          } else {
            values.put(config.name + "_" + x, type);
            order.add(config.name + "_" + x);
          }
        }
      }
    }
    return new Sentence(name, order, values, "raw");
  }


  private static final class Config {
    private final String name;
    private final String type;
    private final String parameters;
    private final int repeats;

    public Config(String name, String type, String parameters, int repeats) {
      this.name = name;
      this.type = type;
      this.parameters = parameters;
      this.repeats = repeats;
    }
  }
}
