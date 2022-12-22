/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.utilities.configuration;

import java.util.Map;
import org.json.JSONObject;

public class YamlParser extends JsonParser {

  public YamlParser(Object mapStructure) {
    json = convertToJson(mapStructure);
  }

  private JSONObject convertToJson(Object yamlLoad) {
    if (yamlLoad instanceof Map) {
      Map<String, Object> map = objectToMap(yamlLoad);
      return new JSONObject(map);
    }
    return new JSONObject();
  }

}