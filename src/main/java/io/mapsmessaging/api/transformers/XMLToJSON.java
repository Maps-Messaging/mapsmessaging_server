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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

public class XMLToJSON implements Transformer {

  private static final Logger logger = LoggerFactory.getLogger(XMLToJSON.class);

  @Override
  public void transform(MessageBuilder messageBuilder) {
    try {
      XmlMapper xmlMapper = new XmlMapper();
      Map<String, Object> map = xmlMapper.readValue(messageBuilder.getOpaqueData(), new TypeReference<>() {});
      JsonObject jsonObject = gson.toJsonTree(map).getAsJsonObject();
      String pretty = gson.toJson(jsonObject);
      messageBuilder.setOpaqueData(pretty.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName());
    }
  }

  @Override
  public Transformer build(ConfigurationProperties properties) {
    return this;
  }

  @Override
  public String getName() {
    return "XMLToJSON";
  }

  @Override
  public String getDescription() {
    return "Converts XML to JSON";
  }
}
