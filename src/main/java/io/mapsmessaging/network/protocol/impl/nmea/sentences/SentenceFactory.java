package io.mapsmessaging.network.protocol.impl.nmea.sentences;

import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class SentenceFactory {

  private final Map<String, SentenceParser> parserMap;

  public SentenceFactory(ConfigurationProperties properties){
    parserMap = new LinkedHashMap<>();
    for(String id:properties.keySet()){
      ConfigurationProperties sentenceConfig = (ConfigurationProperties) properties.get(id);
      SentenceParser sentenceParser = new SentenceParser(id, sentenceConfig);
      parserMap.put(id, sentenceParser);
    }
  }

  public Sentence parse(String id, Iterator<String> entries){
    SentenceParser parser = parserMap.get(id);
    if(parser != null){
      return parser.parse(entries);
    }
    return null;
  }

}
