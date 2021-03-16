/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package org.maps.selector.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.maps.selector.ParseException;
import org.maps.selector.operators.IdentifierResolver;
import org.maps.selector.operators.extentions.ParserExtension;

public class JsonParserExtension implements ParserExtension {

  private final String[] keyPath;

  public JsonParserExtension(){
    keyPath = null;
  }

  protected JsonParserExtension(List<String> arguments) throws ParseException {
    if(arguments.isEmpty()) throw new ParseException("Requires at least 1 argument");
    String key = arguments.get(0);
    if(key.contains(".")){
      StringTokenizer stringTokenizer = new StringTokenizer(key, ".");
      List<String> tmp = new ArrayList<>();
      while(stringTokenizer.hasMoreElements()){
        tmp.add(stringTokenizer.nextElement().toString());
      }
      String[] tmpPath = new String[tmp.size()];
      keyPath = tmp.toArray(tmpPath);
    }
    else{
      keyPath = new String[1];
      keyPath[0] = key;
    }
  }

  @Override
  public ParserExtension createInstance(List<String> arguments) throws ParseException {
    return new JsonParserExtension(arguments);
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
  public Object parse(IdentifierResolver resolver) {
    byte[] payload = resolver.getOpaqueData();
    if (payload != null && payload.length > 0) {
      JSONObject json = new JSONObject(new String(payload));
      if (!json.isEmpty()) {
        Object located = locateObject(json, keyPath);
        return parseJSON(located);
      }
    }
    return null;
  }

  private Object locateObject(JSONObject json, String[] searchPath){
    if(keyPath != null){
      // Walk the JSON path first
      for(int x=0;x<searchPath.length;x++){
        String path = searchPath[x];
        Object jsonObject = json.get(path);
        if(jsonObject instanceof JSONArray){
          String[] sub = new String[searchPath.length-(x +1)];
          System.arraycopy(searchPath, x+1, sub, 0, sub.length);
          return arrayLookup(json.getJSONArray(path), sub);
        }
        else if(jsonObject instanceof JSONObject){
          json = (JSONObject) jsonObject;
        }
        else{
          return jsonObject;
        }
      }
    }
    return null;
  }

  private Object arrayLookup(JSONArray array, String[] path){
    // We have an array, so the next element in the path must be an index ( ie number)
    int idx = Integer.parseInt(path[0]);
    Object lookup = array.get(idx);
    if(lookup instanceof JSONObject){
      String[] sub = new String[path.length-1];
      System.arraycopy(path, 1, sub, 0, sub.length);
      return locateObject( (JSONObject) lookup, sub);
    }
    else if(lookup instanceof JSONArray){
      String[] sub = new String[path.length-1];
      System.arraycopy(path, 1, sub, 0, sub.length);
      return arrayLookup( (JSONArray) lookup, sub);
    }
    return lookup;
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
    return null;
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder("JSON, '");
    for(String path:keyPath){
      sb.append(path).append("' ,");
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof JsonParserExtension){
      JsonParserExtension rhs = (JsonParserExtension)test;
      if(keyPath.length == rhs.keyPath.length){
        for(int x=0;x<keyPath.length;x++){
          if(!keyPath[x].equals(rhs.keyPath[x])){
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode(){
    long largeHash = 0;
    for(String path:keyPath){
      largeHash += path.hashCode();
    }
    return Math.toIntExact(largeHash);
  }

}
