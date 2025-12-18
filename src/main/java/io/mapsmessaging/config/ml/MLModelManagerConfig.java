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

package io.mapsmessaging.config.ml;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.ConfigManager;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.ml.*;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
public class MLModelManagerConfig extends MLModelManagerDTO implements Config, ConfigManager {

  private MLModelManagerConfig(ConfigurationProperties config) {
    this.enableCaching = config.getBooleanProperty("enableCaching", true);
    this.cacheSize = config.getIntProperty("cacheSize", 100);
    this.cacheExpiryMinutes = config.getIntProperty("cacheExpiryMinutes", 60);
    this.preloadModels = convertToList(config.getProperty("preloadModels", ""));

    if (config.get("autoRefresh") != null) {
      ConfigurationProperties autoRefresh = (ConfigurationProperties) config.get("autoRefresh");
      this.autoRefresh = new AutoRefreshConfig();
      this.autoRefresh.setEnabled(autoRefresh.getBooleanProperty("enabled", false));
      this.autoRefresh.setIntervalMinutes(autoRefresh.getIntProperty("intervalMinutes", 10));
    }

    ConfigurationProperties modelStoreConfig = (ConfigurationProperties) config.get("modelStore");
    this.modelStore = new ModelStoreConfig();
    this.modelStore.setType(modelStoreConfig.getProperty("type", null));
    ModelStoreConfigBlock block = new ModelStoreConfigBlock();
    ConfigurationProperties storeConfig = (ConfigurationProperties) modelStoreConfig.get("config");

    switch (modelStoreConfig.getProperty("type", "s3")) {
      case "s3" -> {
        ConfigurationProperties s3Props = (ConfigurationProperties) storeConfig.get("s3");
        S3Config s3 = new S3Config();
        s3.setRegion(s3Props.getProperty("region"));
        s3.setAccessKey(s3Props.getProperty("accessKey"));
        s3.setSecretKey(s3Props.getProperty("secretKey"));
        s3.setBucket(s3Props.getProperty("bucket"));
        s3.setEndpoint(s3Props.getProperty("endpoint"));
        s3.setPrefix(s3Props.getProperty("prefix"));
        block.setS3(s3);
      }
      case "file" -> {
        ConfigurationProperties fileProps = (ConfigurationProperties) storeConfig.get("file");
        var file = new FileConfig();
        file.setPath(fileProps.getProperty("path"));
        block.setFile(file);
      }
      case "nexus" -> {
        ConfigurationProperties nexusProps = (ConfigurationProperties) storeConfig.get("nexus");
        var nexus = new NexusConfig();
        nexus.setUrl(nexusProps.getProperty("url"));
        nexus.setUser(nexusProps.getProperty("user"));
        nexus.setPassword(nexusProps.getProperty("password"));
        block.setNexus(nexus);
      }
      case "maps" ->{
        ConfigurationProperties mapsProps = (ConfigurationProperties) storeConfig.get("maps");
        var maps = new MapsConfig();
        maps.setUrl(mapsProps.getProperty("url"));
        maps.setUser(mapsProps.getProperty("user"));
        maps.setPassword(mapsProps.getProperty("password"));
        block.setMaps(maps);
      }
    }
    if (config.get("eventStreams") != null) {
      this.eventStreams = new ArrayList<>();
      Object eventStreamsObj = config.get("eventStreams");
      List<ConfigurationProperties> list = new ArrayList<>();
      if (eventStreamsObj instanceof List) {
        list = (List<ConfigurationProperties>) eventStreamsObj;
      } else if (eventStreamsObj instanceof ConfigurationProperties) {
        list.add((ConfigurationProperties) eventStreamsObj);
      }
      for (ConfigurationProperties streamProps : list) {
        MLEventStreamDTO dto = new MLEventStreamDTO();
        dto.setId(streamProps.getProperty("id"));
        dto.setTopicFilter(streamProps.getProperty("topicFilter"));
        dto.setSchemaId(streamProps.getProperty("schemaId"));
        dto.setSelector(streamProps.getProperty("selector"));
        dto.setOutlierTopic(streamProps.getProperty("outlierTopic"));
        dto.setMaxTrainEvents(streamProps.getIntProperty("maxTrainEvents", 10000));
        dto.setMaxTrainTimeSeconds(streamProps.getIntProperty("maxTrainTimeSeconds", 0));
        dto.setRetrainThreshold(streamProps.getDoubleProperty("retrainThreshold", 0.05));
        this.eventStreams.add(dto);
      }
    }
    if (config.get("llm") != null) {
      ConfigurationProperties llmProps = (ConfigurationProperties) config.get("llm");
      LlmConfigDTO llm = new LlmConfigDTO();
      llm.setApiToken(llmProps.getProperty("api_token"));
      llm.setModel(llmProps.getProperty("model"));
      this.llmConfig = llm;
    }
    this.modelStore.setConfig(block);
  }

  private List<String> convertToList(String val){
    return new ArrayList<>(Arrays.asList(val.split(",")));
  }

  public static MLModelManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(MLModelManagerConfig.class);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof MLModelManagerDTO)) return false;
    MLModelManagerDTO dto = (MLModelManagerDTO) config;
    boolean changed = false;

    if (this.enableCaching != dto.isEnableCaching()) {
      this.enableCaching = dto.isEnableCaching();
      changed = true;
    }
    if (this.cacheSize != dto.getCacheSize()) {
      this.cacheSize = dto.getCacheSize();
      changed = true;
    }
    if (this.cacheExpiryMinutes != dto.getCacheExpiryMinutes()) {
      this.cacheExpiryMinutes = dto.getCacheExpiryMinutes();
      changed = true;
    }
    if (!equalsSafe(this.preloadModels, dto.getPreloadModels())) {
      this.preloadModels = dto.getPreloadModels();
      changed = true;
    }
    if (!equalsSafe(this.autoRefresh, dto.getAutoRefresh())) {
      this.autoRefresh = dto.getAutoRefresh();
      changed = true;
    }
    if (!equalsSafe(this.modelStore, dto.getModelStore())) {
      this.modelStore = dto.getModelStore();
      changed = true;
    }
    if (!equalsSafe(this.llmConfig, dto.getLlmConfig())) {
      this.llmConfig = dto.getLlmConfig();
      changed = true;
    }
    return changed;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("enableCaching", this.enableCaching);
    props.put("cacheSize", this.cacheSize);
    props.put("cacheExpiryMinutes", this.cacheExpiryMinutes);
    props.put("preloadModels", String.join(",", this.preloadModels));

    if (this.autoRefresh != null) {
      ConfigurationProperties autoRefreshProps = new ConfigurationProperties();
      autoRefreshProps.put("enabled", this.autoRefresh.isEnabled());
      autoRefreshProps.put("intervalMinutes", this.autoRefresh.getIntervalMinutes());
      props.put("autoRefresh", autoRefreshProps);
    }

    if (this.modelStore != null && this.modelStore.getType() != null) {
      ConfigurationProperties storeProps = new ConfigurationProperties();
      storeProps.put("type", this.modelStore.getType());

      ModelStoreConfigBlock cfg = this.modelStore.getConfig();
      ConfigurationProperties storeConfig = new ConfigurationProperties();

      switch (this.modelStore.getType()) {
        case "s3" -> {
          var s3 = cfg.getS3();
          if (s3 != null) {
            ConfigurationProperties s3Props = new ConfigurationProperties();
            s3Props.put("region", s3.getRegion());
            s3Props.put("accessKey", s3.getAccessKey());
            s3Props.put("secretKey", s3.getSecretKey());
            s3Props.put("bucket", s3.getBucket());
            s3Props.put("endpoint", s3.getEndpoint());
            s3Props.put("prefix", s3.getPrefix());
            storeConfig.put("s3", s3Props);
          }
        }
        case "file" -> {
          var file = cfg.getFile();
          if (file != null) {
            ConfigurationProperties fileProps = new ConfigurationProperties();
            fileProps.put("path", file.getPath());
            storeConfig.put("file", fileProps);
          }
        }
        case "nexus" -> {
          var nexus = cfg.getNexus();
          if (nexus != null) {
            ConfigurationProperties nexusProps = new ConfigurationProperties();
            nexusProps.put("url", nexus.getUrl());
            nexusProps.put("user", nexus.getUser());
            nexusProps.put("password", nexus.getPassword());
            storeConfig.put("nexus", nexusProps);
          }
        }
        case "maps" -> {
          var maps = cfg.getMaps();
          if (maps != null) {
            ConfigurationProperties mapsProps = new ConfigurationProperties();
            mapsProps.put("url", maps.getUrl());
            mapsProps.put("user", maps.getUser());
            mapsProps.put("password", maps.getPassword());
            storeConfig.put("maps", mapsProps);
          }
        }
      }
      if (this.llmConfig != null) {
        ConfigurationProperties llmProps = new ConfigurationProperties();
        llmProps.put("api_token", this.llmConfig.getApiToken());
        llmProps.put("model", this.llmConfig.getModel());
        props.put("llm", llmProps);
      }

      storeProps.put("config", storeConfig);
      props.put("modelStore", storeProps);
    }

    return props;
  }



  private boolean equalsSafe(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new MLModelManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }

  @Override
  public String getName() {
    return "MLModelManager";
  }
}
