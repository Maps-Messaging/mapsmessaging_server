package io.mapsmessaging.config.destination;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.storage.StorageConfig;
import io.mapsmessaging.storage.impl.file.config.DeferredConfig;
import io.mapsmessaging.storage.impl.file.config.PartitionStorageConfig;
import io.mapsmessaging.storage.impl.file.config.S3Config;
import io.mapsmessaging.storage.impl.memory.MemoryStorageConfig;
import io.mapsmessaging.storage.impl.tier.memory.MemoryTierConfig;

public class ConfigHelper {

  public static StorageConfig buildConfig(String type, ConfigurationProperties properties) {
    StorageConfig storageConfig = switch (type.toLowerCase()) {
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

  private static PartitionStorageConfig buildPartitionStorageConfig(ConfigurationProperties properties) {
    PartitionStorageConfig partitionStorageConfig = new PartitionStorageConfig();
    partitionStorageConfig.setCapacity( properties.getIntProperty("capacity", -1));
    partitionStorageConfig.setExpiredEventPoll(properties.getIntProperty("expiredEventPoll", 20));
    partitionStorageConfig.setFileName(properties.getProperty("name", ""));
    partitionStorageConfig.setItemCount(properties.getIntProperty("itemCount", 100));
    partitionStorageConfig.setMaxPartitionSize(properties.getLongProperty("maxPartitionSize", 4096L));
    partitionStorageConfig.setSync(properties.getProperty("sync", "disable").equalsIgnoreCase("enable"));

    DeferredConfig dConfig = new DeferredConfig();
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
        s3Config.setRegionName(archive.getProperty("regionName", ""));
        s3Config.setAccessKeyId(archive.getProperty("accessKeyId", ""));
        s3Config.setSecretAccessKey(archive.getProperty("secretAccessKey", ""));
        s3Config.setBucketName(archive.getProperty("bucketName", ""));
        s3Config.setCompression(archive.getBooleanProperty("compression", false));
        dConfig.setS3Config(s3Config);
      }
    }
    partitionStorageConfig.setDeferredConfig(dConfig);
    return partitionStorageConfig;
  }

  private static void packPartitionStorageConfig(ConfigurationProperties properties, PartitionStorageConfig storageConfig) {
    properties.put("capacity", storageConfig.getCapacity());
    properties.put("expiredEventPoll", storageConfig.getExpiredEventPoll());
    properties.put("file", storageConfig.getFileName());
    properties.put("itemCount", storageConfig.getItemCount());
    properties.put("maxPartitionSize", storageConfig.getMaxPartitionSize());
    properties.put("sync", storageConfig.isSync()? "enable" : "disable");
    if(storageConfig.getDeferredConfig() != null) {
      DeferredConfig deferredConfig = storageConfig.getDeferredConfig();
      ConfigurationProperties archive = new ConfigurationProperties();
      archive.put("name", deferredConfig.getDeferredName());
      archive.put("idleTime", deferredConfig.getIdleTime());
      archive.put("digestAlgorithm", deferredConfig.getDigestName());
      archive.put("migrationPath", deferredConfig.getMigrationDestination());
      S3Config s3Config = deferredConfig.getS3Config();
      if(s3Config != null) {
        archive.put("bucketName", s3Config.getBucketName());
        archive.put("accessKeyId", s3Config.getAccessKeyId());
        archive.put("secretAccessKey", s3Config.getSecretAccessKey());
        archive.put("regionName", s3Config.getRegionName());
        archive.put("compression",s3Config.isCompression());
      }
      properties.put("archive", archive);
    }
  }

  private static MemoryStorageConfig buildMemoryStorageConfig(ConfigurationProperties properties) {
    MemoryStorageConfig memoryStorageConfig = new MemoryStorageConfig();
    memoryStorageConfig.setCapacity( properties.getIntProperty("capacity", -1));
    memoryStorageConfig.setExpiredEventPoll(properties.getIntProperty("expiredEventPoll", 20));
    return memoryStorageConfig;
  }

  private static StorageConfig buildMemoryTierStorageConfig(ConfigurationProperties properties) {
    MemoryTierConfig memoryTierConfig = new MemoryTierConfig();
    memoryTierConfig.setMemoryStorageConfig(buildMemoryStorageConfig(properties));
    memoryTierConfig.setPartitionStorageConfig(buildPartitionStorageConfig(properties));
    memoryTierConfig.setMaximumCount(properties.getLongProperty("maximumCount", 0));
    memoryTierConfig.setScanInterval(properties.getLongProperty("scanInterval", 10000));
    memoryTierConfig.setMigrationTime(properties.getLongProperty("migrationTime", 60000));
    return memoryTierConfig;
  }

  public static void packMap(ConfigurationProperties properties, StorageConfig storageConfig) {
    if(storageConfig != null) {
      if(storageConfig instanceof MemoryStorageConfig) {
        packMemoryConfig(properties, (MemoryStorageConfig) storageConfig);
      }
      else if(storageConfig instanceof PartitionStorageConfig) {
        packPartitionStorageConfig(properties, (PartitionStorageConfig)storageConfig);
      }
      else if (storageConfig instanceof MemoryTierConfig){
        packMemoryTierConfig(properties, (MemoryTierConfig) storageConfig);
      }
      properties.put("debug", storageConfig.isDebug());
    }
  }

  private static void packMemoryTierConfig(ConfigurationProperties properties, MemoryTierConfig memoryTierConfig) {
    packMemoryConfig(properties, memoryTierConfig.getMemoryStorageConfig());
    packPartitionStorageConfig(properties, memoryTierConfig.getPartitionStorageConfig());
    properties.put("maximumCount", memoryTierConfig.getMaximumCount());
    properties.put("scanInterval", memoryTierConfig.getScanInterval());
    properties.put("migrationTime", memoryTierConfig.getMigrationTime());
  }

  private static void packMemoryConfig(ConfigurationProperties properties, MemoryStorageConfig storageConfig) {
    properties.put("capacity", storageConfig.getCapacity());
    properties.put("expiredEventPoll", storageConfig.getExpiredEventPoll());
  }

  private ConfigHelper() {
  }
}
