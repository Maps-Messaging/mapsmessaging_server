package io.mapsmessaging.api.message.format;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.mapsmessaging.api.message.format.walker.MapResolver;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class CsvFormat implements Format{

  private final String[] keys;
  private final CsvParser parser;

  public CsvFormat(){
    keys = new String[0];
    parser = null;
  }

  public CsvFormat(ConfigurationProperties properties){
    String keyList = properties.getProperty("header");
    StringTokenizer tokenizer = new StringTokenizer(keyList, ",");
    List<String> header = new ArrayList<>();
    while(tokenizer.hasMoreElements()){
      header.add(tokenizer.nextElement().toString());
    }
    String[] tmp = new String[header.size()];
    keys = header.toArray(tmp);
    CsvParserSettings settings = new CsvParserSettings();
    parser = new CsvParser(settings);
  }

  @Override
  public String getName() {
    return "CSV";
  }

  @Override
  public String getDescription() {
    return "Processes CSV formatted payloads";
  }

  private Map<String, Object> fromByteArray(byte[] payload) throws IOException {
    String[] values = parser.parseLine(new String(payload));
    Map<String, Object> map = new LinkedHashMap<>();
    for(int x=0;x<(Math.min(values.length, keys.length));x++){
      map.put(keys[x], values[x]);
    }
    return map;
  }

  @Override
  public boolean isValid(byte[] payload) {
    try{
      fromByteArray(payload);
      return true;
    }
    catch(IOException ex){
      // ignore
    }
    return false;
  }

  @Override
  public Format getInstance(ConfigurationProperties properties) {
    return new CsvFormat(properties);
  }

  @Override
  public IdentifierResolver getResolver(byte[] payload) throws IOException {
    return new GeneralIdentifierResolver(new MapResolver(fromByteArray(payload)));
  }

}
