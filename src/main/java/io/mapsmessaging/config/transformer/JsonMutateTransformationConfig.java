package io.mapsmessaging.config.transformer;

import com.google.gson.JsonParser;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMutateTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.jsonmutate.JsonMutateOpDTO;
import io.mapsmessaging.dto.rest.config.transformer.jsonmutate.JsonMutateOperation;

import java.util.ArrayList;
import java.util.List;

public class JsonMutateTransformationConfig extends JsonMutateTransformationDTO {

  public JsonMutateTransformationConfig(ConfigurationProperties props) {
    setType(TransformationType.JSON_MUTATE);
    Object rawOperations = props.get("operations");
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
    if (!props.containsKey("op")) {
      return;
    }

    String opValue = props.getProperty("op");
    if (opValue == null) {
      return;
    }

    JsonMutateOperation operation;
    try {
      operation = JsonMutateOperation.valueOf(opValue.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      return;
    }

    JsonMutateOpDTO operationDto = new JsonMutateOpDTO();
    operationDto.setOp(operation);

    if (operation == JsonMutateOperation.REMOVE) {
      operationDto.setPath(props.getProperty("path"));
    } else if (operation == JsonMutateOperation.RENAME) {
      operationDto.setFrom(props.getProperty("from"));
      operationDto.setTo(props.getProperty("to"));
    } else if (operation == JsonMutateOperation.SET) {
      operationDto.setPath(props.getProperty("path"));
      String value = props.getProperty("value");
      if (value != null) {
        operationDto.setValue(JsonParser.parseString(value));
      }
    } else {
      return;
    }

    operations.add(operationDto);
  }

}
