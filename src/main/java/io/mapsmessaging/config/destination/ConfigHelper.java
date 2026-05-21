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

package io.mapsmessaging.config.destination;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.S3Config;
import io.mapsmessaging.dto.rest.config.destination.*;



public class ConfigHelper {

  public static StorageConfigDTO buildConfig(String type, ConfigurationProperties properties) {
    StorageConfigDTO storageConfig = switch (type.toLowerCase()) {
      case "memory" -> buildMemoryStorageConfig(properties);
      case "file", "partition" -> buildPartitionStorageConfig(properties);
      case "memorytier" -> buildMemoryTierStorageConfig(properties);
      default -> null;
    };
    if (storageConfig != null) {
      storageConfig.setDebug(properties.getBooleanProperty("debug", false));
    }
    return storageConfig;
  }

  private static PartitionStorageConfigDTO buildPartitionStorageConfig(ConfigurationProperties properties) {
    PartitionStorageConfigDTO partitionStorageConfig = new PartitionStorageConfigDTO();
    partitionStorageConfig.setCapacity( properties.getIntProperty("capacity", -1));
    partitionStorageConfig.setExpiredEventPoll(properties.getIntProperty("expiredEventPoll", 20));
    partitionStorageConfig.setFileName(properties.getProperty("name", ""));
    partitionStorageConfig.setItemCount(properties.getIntProperty("itemCount", 100));
    partitionStorageConfig.setMaxPartitionSize(properties.getLongProperty("maxPartitionSize", 4096L));
    partitionStorageConfig.setSync(properties.getProperty("sync", "disable").equalsIgnoreCase("enable"));

    DeferredConfigDTO dConfig = new DeferredConfigDTO();
    if (properties.containsKey("archive")) {
      ConfigurationProperties archive = (ConfigurationProperties)properties.get("archive");
      dConfig.setDeferredName(archive.getProperty("name", "None"));
      dConfig.setIdleTime(archive.getLongProperty("idleTime", -1));
      dConfig.setDigestName(archive.getProperty("digestAlgorithm", "MD5"));
      dConfig.setMigrationDestination(archive.getProperty("migrationPath"));
      if (properties.containsKey("s3") || archive.containsKey("s3")) {
        if(archive.containsKey("s3")) {
          archive = (ConfigurationProperties)archive.get("s3");
        }
        S3Config s3Config = new S3Config();
        s3Config.setRegion(archive.getProperty("regionName", ""));
        s3Config.setAccessKey(archive.getProperty("accessKeyId", ""));
        s3Config.setSecretKey(archive.getProperty("secretAccessKey", ""));
        s3Config.setBucket(archive.getProperty("bucketName", ""));
        s3Config.setCompression(archive.getBooleanProperty("compression", false));
        dConfig.setS3Config(s3Config);
      }
    }
    partitionStorageConfig.setDeferredConfig(dConfig);
    return partitionStorageConfig;
  }

  private static void packPartitionStorageConfig(ConfigurationProperties properties, PartitionStorageConfigDTO storageConfig) {
    properties.put("capacity", storageConfig.getCapacity());
    properties.put("expiredEventPoll", storageConfig.getExpiredEventPoll());
    properties.put("file", storageConfig.getFileName());
    properties.put("itemCount", storageConfig.getItemCount());
    properties.put("maxPartitionSize", storageConfig.getMaxPartitionSize());
    properties.put("sync", storageConfig.isSync()? "enable" : "disable");
    if(storageConfig.getDeferredConfig() != null) {
      DeferredConfigDTO deferredConfig = storageConfig.getDeferredConfig();
      ConfigurationProperties archive = new ConfigurationProperties();
      archive.put("name", deferredConfig.getDeferredName());
      archive.put("idleTime", deferredConfig.getIdleTime());
      archive.put("digestAlgorithm", deferredConfig.getDigestName());
      archive.put("migrationPath", deferredConfig.getMigrationDestination());
      S3Config s3Config = deferredConfig.getS3Config();
      if(s3Config != null) {
        archive.put("bucketName", s3Config.getBucket());
        archive.put("accessKeyId", s3Config.getAccessKey());
        archive.put("secretAccessKey", s3Config.getSecretKey());
        archive.put("regionName", s3Config.getRegion());
        archive.put("compression",s3Config.isCompression());
      }
      properties.put("archive", archive);
    }
  }

  private static MemoryStorageConfigDTO buildMemoryStorageConfig(ConfigurationProperties properties) {
    MemoryStorageConfigDTO memoryStorageConfig = new MemoryStorageConfigDTO();
    memoryStorageConfig.setCapacity( properties.getIntProperty("capacity", -1));
    memoryStorageConfig.setExpiredEventPoll(properties.getIntProperty("expiredEventPoll", 20));
    return memoryStorageConfig;
  }

  private static StorageConfigDTO buildMemoryTierStorageConfig(ConfigurationProperties properties) {
    MemoryTierConfigDTO memoryTierConfig = new MemoryTierConfigDTO();
    memoryTierConfig.setMemoryStorageConfig(buildMemoryStorageConfig(properties));
    memoryTierConfig.setPartitionStorageConfig(buildPartitionStorageConfig(properties));
    memoryTierConfig.setMaximumCount(properties.getLongProperty("maximumCount", 0));
    memoryTierConfig.setScanInterval(properties.getLongProperty("scanInterval", 10000));
    memoryTierConfig.setMigrationTime(properties.getLongProperty("migrationTime", 60000));
    return memoryTierConfig;
  }

  public static void packMap(ConfigurationProperties properties, StorageConfigDTO storageConfig) {
    if(storageConfig != null) {
      if(storageConfig instanceof MemoryStorageConfigDTO) {
        packMemoryConfig(properties, (MemoryStorageConfigDTO) storageConfig);
      }
      else if(storageConfig instanceof PartitionStorageConfigDTO) {
        packPartitionStorageConfig(properties, (PartitionStorageConfigDTO)storageConfig);
      }
      else if (storageConfig instanceof MemoryTierConfigDTO){
        packMemoryTierConfig(properties, (MemoryTierConfigDTO) storageConfig);
      }
      properties.put("debug", storageConfig.isDebug());
    }
  }

  private static void packMemoryTierConfig(ConfigurationProperties properties, MemoryTierConfigDTO memoryTierConfig) {
    packMemoryConfig(properties, memoryTierConfig.getMemoryStorageConfig());
    packPartitionStorageConfig(properties, memoryTierConfig.getPartitionStorageConfig());
    properties.put("maximumCount", memoryTierConfig.getMaximumCount());
    properties.put("scanInterval", memoryTierConfig.getScanInterval());
    properties.put("migrationTime", memoryTierConfig.getMigrationTime());
  }

  private static void packMemoryConfig(ConfigurationProperties properties, MemoryStorageConfigDTO storageConfig) {
    properties.put("capacity", storageConfig.getCapacity());
    properties.put("expiredEventPoll", storageConfig.getExpiredEventPoll());
  }

  private ConfigHelper() {
  }
}
