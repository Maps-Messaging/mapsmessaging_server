package io.mapsmessaging.api.message.format.walker;

import java.util.List;
import java.util.Map;

public class MapResolver implements Resolver {

  private final Map<String, Object> map;

  public MapResolver(Map<String, Object> map){
    this.map = map;
  }

  @Override
  public Object get(String s) {
    String lookup = s;
    boolean isArray = false;
    if(s.endsWith("]")){
      lookup = s.substring(0, s.indexOf("["));
      isArray = true;
    }
    if(map.containsKey(lookup)) {
      Object val = map.get(lookup);
      if(val instanceof List && isArray){
        String index = s.substring(s.indexOf("[")+1, s.indexOf("]"));
        var idx = Integer.parseInt(index.trim());
        val = ((List)val).get(idx);
      }
      if(val instanceof Map){
        return new MapResolver((Map)val);
      }
      return val;
    }
    return null;
  }
}