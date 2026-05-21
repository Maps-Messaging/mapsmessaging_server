package io.mapsmessaging.tools.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.mapsmessaging.config.ConfigManager;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.ConfigNamingDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.util.ArrayList;

public class UpgradeTest {

  public static void main(String[] args) {
    ObjectMapper objectMapper = buildObjectMapper();

    ConfigurationManager configurationManager = ConfigurationManager.getInstance();
    configurationManager.setFeatureManager(new FeatureManager(new ArrayList<>()));
    configurationManager.initialise("fred");
    configurationManager.loadAll();

    for (ConfigNamingDTO managerName : configurationManager.getKnownManagers()) {
      try {
        ConfigManager manager = configurationManager.getManager(managerName.getConfigName());
        manager.load(new FeatureManager(new ArrayList<>()));

        if(manager instanceof BaseConfigDTO dto){
          JsonNode originalJson = objectMapper.valueToTree(dto);
          if (originalJson.isObject()) {
            ((ObjectNode) originalJson).remove("name");
          }

          @SuppressWarnings("unchecked")
          Class<? extends BaseConfigDTO> managerClass = (Class<? extends BaseConfigDTO>) manager.getClass().getSuperclass();

          BaseConfigDTO managerCopy = objectMapper.treeToValue(originalJson, managerClass);

          JsonNode copyJson = objectMapper.valueToTree(managerCopy);
          if (copyJson.isObject()) {
            ((ObjectNode) copyJson).remove("name");
          }
          if (!originalJson.equals(copyJson)) {
            System.err.println("Manager: " + managerName
                + "\n Original: " + toPretty(objectMapper, originalJson)
                + "\n Copy    : " + toPretty(objectMapper, copyJson)
                + "\n Equal   : " + originalJson.equals(copyJson));
          }
          else{
            System.err.println("Manager: " + managerName + " is OK");
          }
        }

      } catch (Exception exception) {
        System.err.println("Error processing manager: " + managerName);
        exception.printStackTrace();
        // keep swallowing, as per your original test
      }
    }
  }

  private static ObjectMapper buildObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    return objectMapper;
  }

  private static String toPretty(ObjectMapper objectMapper, JsonNode jsonNode) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
    } catch (JsonProcessingException exception) {
      return jsonNode.toString();
    }
  }
}
