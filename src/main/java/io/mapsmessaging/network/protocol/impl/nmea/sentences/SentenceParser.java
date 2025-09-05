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

package io.mapsmessaging.network.protocol.impl.nmea.sentences;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.protocol.impl.nmea.types.EnumTypeFactory;
import io.mapsmessaging.network.protocol.impl.nmea.types.Type;
import io.mapsmessaging.network.protocol.impl.nmea.types.TypeFactory;
import lombok.Getter;

import java.util.*;

@Getter
public class SentenceParser {

  private final String name;
  private final String description;
  private final List<Config> configs;
  private final String alias;

  public SentenceParser(String name, ConfigurationProperties config) {
    this.name = name;
    this.configs = new ArrayList<>();
    this.description = config.getProperty("description");
    alias = config.getProperty("aliasOf", null);
    ConfigurationProperties syntax = (ConfigurationProperties) config.get("syntax");
    int idx = 1;
    if(syntax != null) {
      ConfigurationProperties entry = (ConfigurationProperties) syntax.get("" + idx);
      while (entry != null) {
        String valueName = entry.getProperty("name");
        String typeName = entry.getProperty("type");
        String param = entry.getProperty("param", "");
        int repeats = entry.getIntProperty("repeat", 1);

        if (!param.isEmpty() && typeName.equalsIgnoreCase("enum")) {
          EnumTypeFactory.getInstance().register(valueName, param);
        }
        this.configs.add(new Config(valueName, typeName, param, repeats));
        idx++;
        entry = (ConfigurationProperties) syntax.get("" + idx);
      }
    }
  }

  public Sentence parse(Iterator<String> entries) {
    List<String> order = new ArrayList<>();
    Map<String, Type> values = new LinkedHashMap<>();
    for (Config config : configs) {
      if (entries.hasNext()) {
        int repeat = config.repeats;
        parseEntry(config, entries, repeat, values, order);
      }
    }
    return new Sentence(name, description, order, values);
  }

  private void parseEntry(Config config, Iterator<String> entries, int repeat, Map<String, Type> values, List<String> order) {
    for (int x = 0; x < repeat; x++) {
      Type type = TypeFactory.create(config.name, config.type, config.parameters, entries);
      if (type != null) {
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
