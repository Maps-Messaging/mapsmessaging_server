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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.Protocol;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

@SuppressWarnings("java:S2129") // We convert a Byte[] into a String for json to parse
public class JSONToXML implements InterServerTransformation {
  private static final Logger logger = LoggerFactory.getLogger(JSONToXML.class);

  @Override
  public Protocol.ParsedMessage transform(String source, Protocol.ParsedMessage message){
    MessageBuilder messageBuilder = new MessageBuilder(message.getMessage());
    convert(messageBuilder);
    message.setMessage(messageBuilder.build());
    return message;
  }

  @Override
  public InterServerTransformation build(ConfigurationProperties properties) {
    return this;
  }

  @Override
  public String getName() {
    return "JSONToXML";
  }

  @Override
  public String getDescription() {
    return "Converts JSON to XML";
  }


  private void convert(MessageBuilder messageBuilder) {
    try {
      JsonObject jsonObject = JsonParser.parseString(
          new String(messageBuilder.getOpaqueData(), StandardCharsets.UTF_8)
      ).getAsJsonObject();

      // Convert JsonObject to Map for XmlMapper
      Type type = new TypeToken<Map<String, Object>>() {}.getType();
      Map<String, Object> map = gson.fromJson(jsonObject, type);

      XmlMapper xmlMapper = new XmlMapper();
      String xml = xmlMapper.writeValueAsString(map);

      messageBuilder.setOpaqueData(xml.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName());
    }
  }

}
