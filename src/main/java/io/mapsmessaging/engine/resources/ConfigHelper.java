package io.mapsmessaging.engine.resources;

import io.mapsmessaging.dto.rest.config.destination.ArchiveConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.S3ArchiveConfigDTO;
import io.mapsmessaging.storage.StorageConfig;
import io.mapsmessaging.storage.impl.file.PartitionStorageConfig;
import io.mapsmessaging.storage.impl.file.S3Config;
import io.mapsmessaging.storage.impl.memory.MemoryStorageConfig;
import io.mapsmessaging.storage.impl.tier.memory.MemoryTierConfig;

public class ConfigHelper {

  public static StorageConfig buildConfig(String fileName, DestinationConfigDTO destinationConfigDTO) {
    StorageConfig storageConfig = switch (destinationConfigDTO.getType().toLowerCase()) {
      case "memory" -> buildMemoryStorageConfig(destinationConfigDTO);
      case "file", "partition" -> buildPartitionStorageConfig(fileName, destinationConfigDTO);
      case "memorytier" -> buildMemoryTierStorageConfig(fileName, destinationConfigDTO);
      default -> null;
    };
    if (storageConfig != null) {
      storageConfig.setDebug(destinationConfigDTO.isDebug());
    }
    return storageConfig;
  }

  private static PartitionStorageConfig buildPartitionStorageConfig(String fileName, DestinationConfigDTO destinationConfigDTO) {
    PartitionStorageConfig partitionStorageConfig = new PartitionStorageConfig();
    partitionStorageConfig.setCapacity(destinationConfigDTO.getCapacity());
    partitionStorageConfig.setExpiredEventPoll(destinationConfigDTO.getExpiredEventPoll());
    partitionStorageConfig.setFileName(destinationConfigDTO.getName());
    partitionStorageConfig.setItemCount(destinationConfigDTO.getItemCount());
    partitionStorageConfig.setMaxPartitionSize(destinationConfigDTO.getMaxPartitionSize());
    partitionStorageConfig.setSync(destinationConfigDTO.isSync());
    partitionStorageConfig.setFileName(fileName);

    ArchiveConfigDTO archiveConfigDTO = destinationConfigDTO.getArchive();
    if (archiveConfigDTO != null) {
      S3ArchiveConfigDTO s3ArchiveConfigDTO = archiveConfigDTO.getS3();
      if (s3ArchiveConfigDTO != null) {
        S3Config s3Config = new S3Config();
        s3Config.setAccessKeyId(s3ArchiveConfigDTO.getAccessKeyId());
        s3Config.setSecretAccessKey(s3ArchiveConfigDTO.getSecretAccessKey());
        s3Config.setRegionName(s3ArchiveConfigDTO.getRegionName());
        s3Config.setBucketName(s3ArchiveConfigDTO.getBucketName());
        s3Config.setCompression(s3ArchiveConfigDTO.isCompression());
        partitionStorageConfig.setS3Config(s3Config);
      }
      partitionStorageConfig.setMigrationDestination(archiveConfigDTO.getMigrationPath());
      partitionStorageConfig.setArchiveName(archiveConfigDTO.getName());
      partitionStorageConfig.setDigestName(archiveConfigDTO.getDigestAlgorithm());
      partitionStorageConfig.setArchiveIdleTime(archiveConfigDTO.getIdleTime());
    }
    return partitionStorageConfig;
  }

  private static MemoryStorageConfig buildMemoryStorageConfig(DestinationConfigDTO destinationConfigDTO) {
    MemoryStorageConfig memoryStorageConfig = new MemoryStorageConfig();
    memoryStorageConfig.setCapacity(destinationConfigDTO.getCapacity());
    memoryStorageConfig.setExpiredEventPoll(destinationConfigDTO.getExpiredEventPoll());
    return memoryStorageConfig;
  }

  private static StorageConfig buildMemoryTierStorageConfig(String fileName, DestinationConfigDTO destinationConfigDTO) {
    MemoryTierConfig memoryTierConfig = new MemoryTierConfig();
    memoryTierConfig.setMemoryStorageConfig(buildMemoryStorageConfig(destinationConfigDTO));
    memoryTierConfig.setPartitionStorageConfig(buildPartitionStorageConfig(fileName, destinationConfigDTO));
    memoryTierConfig.setMaximumCount(destinationConfigDTO.getMaxPartitionSize());
    return memoryTierConfig;
  }

  private ConfigHelper() {
  }
}
