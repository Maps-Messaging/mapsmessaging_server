package io.mapsmessaging.network.protocol.impl.nmea.types;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class EnumTypeFactory {

  private static final EnumTypeFactory instance = new EnumTypeFactory();

  private final Map<String, Map<String, String>> configuration;

  public static EnumTypeFactory getInstance(){
    return instance;
  }

  public synchronized void register(String name, String jsonObjectOptions){
    JSONArray jsonArray = new JSONArray(jsonObjectOptions);
    Map<String, String> map = new LinkedHashMap<>();
    for(int x=0;x<jsonArray.length();x++) {
      JSONObject jsonObject = jsonArray.getJSONObject(x);
      for (String key : jsonObject.keySet()) {
        map.put(key, jsonObject.get(key).toString());
      }
    }
    configuration.put(name, map);
  }

  public synchronized EnumType getEnum(String name, String id){
    Map<String, String> map = configuration.get(name);
    if(map != null){
      String desc = map.get(id);
      return new EnumType(id, desc);
    }
    return null;
  }


  private EnumTypeFactory(){
    configuration = new LinkedHashMap<>();
  }

}
