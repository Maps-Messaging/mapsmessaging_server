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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class SentenceFactory {

  private final Map<String, SentenceParser> parserMap;

  public SentenceFactory(ConfigurationProperties properties) {
    parserMap = new LinkedHashMap<>();
    for (Map.Entry<String, Object> configEntry : properties.entrySet()) {
      SentenceParser sentenceParser = new SentenceParser(configEntry.getKey(), (ConfigurationProperties) configEntry.getValue());
      parserMap.put(configEntry.getKey(), sentenceParser);
    }
    // Now lets process the aliases
    for(Map.Entry<String, SentenceParser> entry : parserMap.entrySet()) {
      SentenceParser sentenceParser = entry.getValue();
      if(sentenceParser.getAlias() != null) {
        SentenceParser alias = parserMap.get(sentenceParser.getAlias());
        if(alias != null && sentenceParser.getConfigs().isEmpty()) {
          sentenceParser.getConfigs().addAll(alias.getConfigs()); // Load the alias config to be parsed
        }
      }
    }
  }

  public Sentence parse(String id, Iterator<String> entries) {
    SentenceParser parser = parserMap.get(id);
    if (parser != null) {
      return parser.parse(entries);
    }
    return null;
  }

}
