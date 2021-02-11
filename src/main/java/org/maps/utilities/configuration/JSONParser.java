package org.maps.utilities.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONObject;

public class JSONParser {

  protected JSONObject Json;

  protected JSONParser(){}

  public  JSONParser(JSONObject json){
    Json = json;
  }

  public JSONObject getJson() {
    return Json;
  }

  public Map<String, Object> parse()throws IOException {
    Map<String, Object> result = (Map<String, Object>) new ObjectMapper().readValue(Json.toString(2), LinkedHashMap.class);
    Map<String, Object> map = removeUnnecessaryLists(result);
    return new ConfigurationProperties(map);
  }

  private Map<String, Object> removeUnnecessaryLists(Map<String, Object> map){
    for(Entry<String, Object> entry:map.entrySet()){
      if(entry.getValue() instanceof List){
        List<Object> list = objectToList(entry.getValue());
        if(list.size() == 1){
          entry.setValue(list.remove(0));
        }
        if(entry.getValue() instanceof Map){
          entry.setValue(removeUnnecessaryLists(objectToMap(entry.getValue())));
        }
      }
      else if(entry.getValue() instanceof Map){
        entry.setValue(removeUnnecessaryLists(objectToMap(entry.getValue())));
      }
    }
    return map;
  }


  protected List<Object> objectToList(Object list){
    return (List<Object>) list;
  }

  protected Map<String, Object> objectToMap(Object list){
    return (Map<String, Object>) list;
  }
}
