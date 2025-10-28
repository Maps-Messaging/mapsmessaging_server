/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.api.transformers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.extensions.JsonParserExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class JsonToValueTransformation implements InterServerTransformation {

  private final JsonParserExtension jsonParser;

  public JsonToValueTransformation() {
    jsonParser = null;
  }

  public JsonToValueTransformation(String key){
    JsonParserExtension parser = null;
    try {
      parser = new JsonParserExtension(List.of(key));
    } catch (ParseException e) {
      // ToDo log exception
    }
    jsonParser = parser;
  }

  public InterServerTransformation build(ConfigurationProperties properties) {
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
      JsonObject temp = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
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
  public Protocol.ParsedMessage transform(String source, Protocol.ParsedMessage message){
    MessageBuilder messageBuilder = new MessageBuilder(message.getMessage());
    messageBuilder.setOpaqueData(convert(message.getMessage().getOpaqueData()));
    message.setMessage(messageBuilder.build());
    return message;
  }

}
