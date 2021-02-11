package org.maps.utilities.configuration;

import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class YamlParser extends JSONParser {


  public YamlParser(Object mapStructure) {
    Json = convertToJson(mapStructure);
  }

  private JSONObject convertToJson(Object yamlLoad) {
    if(yamlLoad instanceof Map) {
      Map<String, Object> map = objectToMap(yamlLoad);
      return new JSONObject(map);
    }
    else if(yamlLoad instanceof List){
      JSONObject jsonObject = new JSONObject();
      JSONArray jsonArray = new JSONArray();
      List<Object> list = objectToList(yamlLoad);
      for(Object entry:list){
        jsonArray.put(entry);
      }
      jsonObject.put("", jsonArray);
      return jsonObject;
    }
    return new JSONObject();
  }

}
