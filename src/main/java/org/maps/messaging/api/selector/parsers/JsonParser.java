/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.api.selector.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.parsers.SelectorParser;

public class JsonParser implements SelectorParser {

  private final String keyName;
  private final String[] keyPath;
  private final int arrayIndex;

  public JsonParser(){
    keyName = null;
    keyPath = null;
    arrayIndex = -1;
  }

  protected JsonParser(List<String> arguments) throws ParseException {
    if(arguments.isEmpty()) throw new ParseException("Requires at least 1 argument");
    String key = arguments.get(0);
    if(arguments.size() > 1){
      arrayIndex = Integer.parseInt(arguments.get(1));
    }
    else{
      arrayIndex = -1;
    }
    if(key.contains(".")){
      StringTokenizer stringTokenizer = new StringTokenizer(key, ".");
      List<String> tmp = new ArrayList<>();
      while(stringTokenizer.hasMoreElements()){
        tmp.add(stringTokenizer.nextElement().toString());
      }
      keyName = tmp.remove(tmp.size()-1);
      String[] tmpPath = new String[tmp.size()];
      keyPath = tmp.toArray(tmpPath);
    }
    else{
      keyPath = null;
      keyName = key;
    }
  }

  @Override
  public SelectorParser createInstance(List<String> arguments) throws ParseException {
    return new JsonParser(arguments);
  }

  @Override
  public String getName(){
    return "json";
  }

  @Override
  public String getDescription() {
    return "Parses the byte[] as a JSON object to enable filtering via the JSON object";
  }

  @Override
  public Object parse(Message message) {
    byte[] payload = message.getOpaqueData();
    if (payload != null && payload.length > 0) {
      JSONObject json = new JSONObject(new String(payload));
      if (!json.isEmpty()) {
        return locateObject(json);
      }
    }
    return null;
  }

  private Object locateObject(JSONObject json){
    if(keyPath != null){
      // Walk the JSON path first
      for(String path:keyPath){
        json = json.getJSONObject(path);
        if(json == null){
          return null;
        }
      }
    }
    return parseJSON(json.get(keyName));

  }

  private Object parseJSON(Object lookup){
    if (lookup instanceof String ||
        lookup instanceof Float ||
        lookup instanceof Double ||
        lookup instanceof Byte ||
        lookup instanceof Short ||
        lookup instanceof Integer ||
        lookup instanceof Long) {
      return lookup;
    }
    if(lookup instanceof JSONArray && arrayIndex > -1){
      return parseJSON(((JSONArray)lookup).get(arrayIndex));
    }
    return null;
  }

  @Override
  public String toString(){
    if(arrayIndex > -1) {
      return "JSON, '" + keyName+"', "+arrayIndex;
    }
    return "JSON, '" + keyName+"'";
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof JsonParser){
      return keyName.equals(((JsonParser) test).keyName) && arrayIndex == ((JsonParser)test).arrayIndex;
    }
    return false;
  }

  @Override
  public int hashCode(){
    return keyName.hashCode() | arrayIndex;
  }

}
