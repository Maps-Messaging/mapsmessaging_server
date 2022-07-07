/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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
 */

package io.mapsmessaging.network.protocol.impl.nmea.types;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class EnumTypeFactory {

  private static final EnumTypeFactory instance = new EnumTypeFactory();

  private final Map<String, Map<String, String>> configuration;

  public static EnumTypeFactory getInstance() {
    return instance;
  }

  public synchronized void register(String name, String jsonObjectOptions) {
    JSONArray jsonArray = new JSONArray(jsonObjectOptions);
    Map<String, String> map = new LinkedHashMap<>();
    for (int x = 0; x < jsonArray.length(); x++) {
      JSONObject jsonObject = jsonArray.getJSONObject(x);
      for (String key : jsonObject.keySet()) {
        map.put(key, jsonObject.get(key).toString());
      }
    }
    configuration.put(name, map);
  }

  public synchronized EnumType getEnum(String name, String id) {
    Map<String, String> map = configuration.get(name);
    if (map != null) {
      String desc = map.get(id);
      return new EnumType(id, desc);
    }
    return null;
  }


  private EnumTypeFactory() {
    configuration = new LinkedHashMap<>();
  }

}
