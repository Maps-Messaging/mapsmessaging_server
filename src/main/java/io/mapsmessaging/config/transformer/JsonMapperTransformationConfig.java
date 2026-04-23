/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.config.transformer;

import com.google.gson.JsonParser;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMapperTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.jsonmapper.JsonMapFunction;
import io.mapsmessaging.dto.rest.config.transformer.jsonmapper.JsonMapOpDTO;

import java.util.ArrayList;
import java.util.List;

public class JsonMapperTransformationConfig extends JsonMapperTransformationDTO {

  public JsonMapperTransformationConfig(ConfigurationProperties props) {
    setType(TransformationType.JSON_MAPPER);
    ConfigurationProperties jsonMapper = (ConfigurationProperties) props.get("jsonMapper");
    Object rawOperations = jsonMapper.get("operations");
    if(rawOperations != null){
      operations = new ArrayList<>();
      if(rawOperations instanceof ConfigurationProperties operationProperties){
        buildOperation(operationProperties);
      }
      else if(rawOperations instanceof List list){
        for(Object operation : list){
          if(operation instanceof ConfigurationProperties operationProperties){
            buildOperation(operationProperties);
          }
        }
      }
    }
  }

  private void buildOperation(ConfigurationProperties props) {
    if (!props.containsKey("from") || !props.containsKey("to")) {
      return;
    }

    String from = props.getProperty("from");
    String to = props.getProperty("to");
    if (from == null || to == null) {
      return;
    }

    JsonMapOpDTO operation = new JsonMapOpDTO();
    operation.setFrom(from);
    operation.setTo(to);
    operation.setFunction(JsonMapFunction.fromString(props.getProperty("function", "NONE")));
    operation.setDefaultValue(operation.getDefaultValue());
    operations.add(operation);
  }

}
