package io.mapsmessaging.dto.rest.config.destination;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MemoryTierConfigDTO extends StorageConfigDTO {
  @Schema(
      description = "Time in milliseconds after which data is migrated from memory to disk",
      defaultValue = "60000",
      minimum = "1000",
      maximum = "86400000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  private long migrationTime;

  @Schema(
      description = "Interval in milliseconds to scan for data eligible for migration",
      defaultValue = "10000",
      minimum = "1000",
      maximum = "86400000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  private long scanInterval;

  @Schema(
      description = "Maximum number of events to retain in memory before migration",
      defaultValue = "0",
      minimum = "0",
      maximum = "1000000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  private long maximumCount;

  @Schema(
      description = "Configuration for the in-memory tier",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  private MemoryStorageConfigDTO memoryStorageConfig;

  @Schema(
      description = "Configuration for the disk-backed partition tier",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  private PartitionStorageConfigDTO partitionStorageConfig;

  public MemoryTierConfigDTO(){
    super("memoryTier");
  }
}
