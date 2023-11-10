/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.extensions.JsonParserExtension;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import org.json.JSONObject;

import java.util.List;

public class JsonToValueTransformation implements Transformer {

  private final JsonParserExtension jsonParser;

  public JsonToValueTransformation() {
    jsonParser = null;
  }

  public JsonToValueTransformation(String key){
    JsonParserExtension parser = null;
    try {
      parser = new JsonParserExtension(List.of(key));
    } catch (ParseException e) {
      // To Do
      parser = null;
    }
    jsonParser = parser;
  }

  public Transformer build(ConfigurationProperties properties) {
    String property = properties != null ?
        properties.getProperty("key", properties.getProperty("data")) :
        null;
    return property != null ? new JsonToValueTransformation(property) : new JsonToValueTransformation();
  }

  @Override
  public String getName() {
    return "JsonToValue";
  }

  @Override
  public String getDescription() {
    return "Retrieves a specific Json Value";
  }


  private byte[] convert(byte[] data) {
    try{
      JSONObject temp = new JSONObject(new String(data));
      if(jsonParser != null){
        Object v = jsonParser.locateObject(temp);
        if (v != null) {
          String val = v.toString();
          return val.getBytes();
        }
      }
    }
    catch(Exception ex){
      // Add log
    }
    return data;
  }

  @Override
  public void transform(MessageBuilder messageBuilder) {
    messageBuilder.setOpaqueData(convert(messageBuilder.getOpaqueData()));
  }
}
