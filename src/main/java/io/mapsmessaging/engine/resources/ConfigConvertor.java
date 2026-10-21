package io.mapsmessaging.engine.resources;

import io.mapsmessaging.dto.rest.config.destination.*;
import io.mapsmessaging.storage.StorageConfig;
import io.mapsmessaging.storage.impl.file.config.DeferredConfig;
import io.mapsmessaging.storage.impl.file.config.PartitionStorageConfig;
import io.mapsmessaging.storage.impl.file.config.S3Config;
import io.mapsmessaging.storage.impl.memory.MemoryStorageConfig;
import io.mapsmessaging.storage.impl.tier.memory.MemoryTierConfig;

public class ConfigConvertor {


  public static StorageConfig convert(StorageConfigDTO config) {
    if(config instanceof MemoryStorageConfigDTO memoryConfigDTO) {
      MemoryStorageConfig memoryStorageConfig = new MemoryStorageConfig();
      memoryStorageConfig.setCapacity(memoryConfigDTO.getCapacity());
      memoryStorageConfig.setExpiredEventPoll(memoryConfigDTO.getExpiredEventPoll());
      memoryStorageConfig.setType(memoryConfigDTO.getType());
      memoryStorageConfig.setDebug(config.isDebug());
      memoryStorageConfig.setType("Memory");
      return memoryStorageConfig;

    }
    else if(config instanceof PartitionStorageConfigDTO partitionStorageConfigDTO) {
      PartitionStorageConfig partitionStorageConfig = new PartitionStorageConfig();
      partitionStorageConfig.setType(partitionStorageConfigDTO.getType());
      partitionStorageConfig.setDebug(config.isDebug());
      partitionStorageConfig.setCapacity(partitionStorageConfigDTO.getCapacity());
      partitionStorageConfig.setExpiredEventPoll(partitionStorageConfigDTO.getExpiredEventPoll());
      partitionStorageConfig.setMaxPartitionSize(partitionStorageConfigDTO.getMaxPartitionSize());
      partitionStorageConfig.setItemCount(partitionStorageConfigDTO.getItemCount());
      partitionStorageConfig.setDeferredConfig(convert(partitionStorageConfigDTO.getDeferredConfig()));
      partitionStorageConfig.setSync(partitionStorageConfigDTO.isSync());
      partitionStorageConfig.setType("Partition");
      return partitionStorageConfig;
    }
    else if(config instanceof MemoryTierConfigDTO memoryTierConfigDTO) {
      MemoryTierConfig memoryTierConfig = new MemoryTierConfig();
      memoryTierConfig.setDebug(config.isDebug());
      memoryTierConfig.setType(memoryTierConfigDTO.getType());
      memoryTierConfig.setMaximumCount(memoryTierConfigDTO.getMaximumCount());
      memoryTierConfig.setMigrationTime(memoryTierConfigDTO.getMigrationTime());
      memoryTierConfig.setScanInterval(memoryTierConfigDTO.getScanInterval());
      memoryTierConfig.setMemoryStorageConfig((MemoryStorageConfig) convert(memoryTierConfigDTO.getMemoryStorageConfig()));
      memoryTierConfig.setPartitionStorageConfig((PartitionStorageConfig) convert(memoryTierConfigDTO.getPartitionStorageConfig()));
      memoryTierConfig.setType("MemoryTier");
      return memoryTierConfig;
    }
    return null;
  }


  public static DeferredConfig convert(DeferredConfigDTO config) {
    DeferredConfig deferredConfig = new DeferredConfig();
    deferredConfig.setDeferredName(config.getDeferredName());
    deferredConfig.setDigestName(config.getDigestName());
    deferredConfig.setIdleTime(config.getIdleTime());
    deferredConfig.setMigrationDestination(config.getMigrationDestination());
    if(config.getS3Config() != null) {
      deferredConfig.setS3Config(convert(config.getS3Config()));
    }
    return deferredConfig;
  }

  public static S3Config convert(io.mapsmessaging.dto.rest.config.S3Config config) {
    S3Config s3Config = new S3Config();
    s3Config.setBucketName(config.getBucket());
    s3Config.setRegionName(config.getRegion());
    s3Config.setAccessKeyId(config.getAccessKey());
    s3Config.setSecretAccessKey(config.getSecretKey());
    s3Config.setCompression(config.isCompression());
    return s3Config;
  }


  private ConfigConvertor() {}
}
