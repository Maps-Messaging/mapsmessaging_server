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


package io.mapsmessaging.config;


import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.schema.*;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.io.IOException;
import java.util.List;

public class SchemaManagerConfig extends SchemaManagerConfigDTO implements Config, ConfigManager {

  public SchemaManagerConfig() {}

  public SchemaManagerConfig(ConfigurationProperties configurationProperties) {
    String type = configurationProperties.getProperty("type", "simple");
    switch (type) {
      case "simple" -> super.setRepositoryConfig(new SimpleRepositoryConfigDTO());
      case "file" -> super.setRepositoryConfig(configureFile( (ConfigurationProperties) configurationProperties.get("config")));
      case "maps" -> super.setRepositoryConfig(configureMaps( (ConfigurationProperties) configurationProperties.get("config")));
      default -> super.setRepositoryConfig(new SimpleRepositoryConfigDTO());
    }
    Object locations = configurationProperties.get("importLocations");
    if(locations instanceof List list){
      for(Object location : list){
        if(location instanceof ConfigurationProperties props){
          loadLocations(props);
        }
      }
    }
    else if(locations instanceof ConfigurationProperties props){
      loadLocations(props);
    }
    setProtocPath(configurationProperties.getProperty("protocPath", null));
    String parseModeString = configurationProperties.getProperty("parseMode", "IGNORE");
    setParseMode(ParseMode.valueOf(parseModeString));
  }

  private void loadLocations(ConfigurationProperties props){
    if(props.containsKey("path") && props.containsKey("format") && props.containsKey("name")){
      SchemaImportLocationDTO locationDTO = new SchemaImportLocationDTO();
      locationDTO.setPath(props.getProperty("path"));
      locationDTO.setFormat(props.getProperty("format"));
      locationDTO.setName(props.getProperty("name"));
      super.getImportLocations().add(locationDTO);
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    return null;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    return false;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new SchemaManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {

  }

  @Override
  public String getName() {
    return "SchemaManager";
  }

  private RepositoryConfigDTO configureFile(ConfigurationProperties properties){
    FileRepositoryConfigDTO config = new FileRepositoryConfigDTO();
    config.setDirectoryPath(properties.getProperty("directoryPath"));
    return config;
  }

  private RepositoryConfigDTO configureMaps(ConfigurationProperties properties){
    MapsRepositoryConfigDTO mapsRepositoryConfigDTO = new MapsRepositoryConfigDTO();
    mapsRepositoryConfigDTO.setDirectoryPath(properties.getProperty("directoryPath"));
    mapsRepositoryConfigDTO.setPullSchemas(properties.getBooleanProperty("pullSchemas", true));
    mapsRepositoryConfigDTO.setPushSchemas(properties.getBooleanProperty("pushSchemas", true));
    mapsRepositoryConfigDTO.setUrlPath(properties.getProperty("urlPath"));
    mapsRepositoryConfigDTO.setUsername(properties.getProperty("username"));
    mapsRepositoryConfigDTO.setPassword(properties.getProperty("password"));
    return mapsRepositoryConfigDTO;
  }
}