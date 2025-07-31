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

package io.mapsmessaging.ml;

import io.mapsmessaging.config.ml.MLModelManagerConfig;
import io.mapsmessaging.dto.rest.config.ml.FileConfig;
import io.mapsmessaging.dto.rest.config.ml.ModelStoreConfig;
import io.mapsmessaging.dto.rest.config.ml.NexusConfig;
import io.mapsmessaging.dto.rest.config.ml.S3Config;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.selector.ml.ModelStore;
import io.mapsmessaging.selector.ml.impl.store.FileModelStore;
import io.mapsmessaging.selector.ml.impl.store.NexusModelStore;
import io.mapsmessaging.selector.ml.impl.store.S3ModelStore;
import io.mapsmessaging.selector.operators.functions.ml.MLFunction;
import io.mapsmessaging.utilities.Agent;
import software.amazon.awssdk.regions.Region;

public class MLModelManager implements Agent {

  private CachingModelStore modelStore;

  public MLModelManager(){

  }

  @Override
  public String getName() {
    return "MLModelManager";
  }

  @Override
  public String getDescription() {
    return "Manages the ML Model stores and ML features";
  }

  @Override
  public void start() {
    MLModelManagerConfig mlModelManagerConfig = MLModelManagerConfig.getInstance();
    ModelStoreConfig modelStoreConfig = mlModelManagerConfig.getModelStore();
    if(modelStoreConfig != null) {
      ModelStore store = buildModelStore(modelStoreConfig);
      if (store != null) {
        modelStore = new CachingModelStore(store);
        MLFunction.setModelStore(modelStore);
      }
    }
  }

  @Override
  public void stop() {
    modelStore.stop();
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    return null;
  }

  private ModelStore buildModelStore(ModelStoreConfig config) {
    if(config.getType().equalsIgnoreCase("s3")) {
      S3Config s3Config = config.getConfig().getS3();
      return new S3ModelStore(
          s3Config.getBucket(),
          "/",
          Region.of(s3Config.getRegion()),
          s3Config.getAccessKey(),
          s3Config.getSecretKey()
      );
    }
    else if(config.getType().equalsIgnoreCase("file")) {
      FileConfig fileConfig = config.getConfig().getFile();
      return new FileModelStore(fileConfig.getPath());
    }
    else if(config.getType().equalsIgnoreCase("nexus")) {
      NexusConfig nexusConfig = config.getConfig().getNexus();
      NexusModelStore nexusModelStore = new NexusModelStore(nexusConfig.getUrl());
      if(nexusConfig.getUser() != null && !nexusConfig.getUser().isEmpty() &&
          nexusConfig.getPassword() != null && !nexusConfig.getPassword().isEmpty()) {
        nexusModelStore.login(nexusConfig.getUser(), nexusConfig.getPassword());
      }
      return nexusModelStore;
    }
    return null;
  }
}
