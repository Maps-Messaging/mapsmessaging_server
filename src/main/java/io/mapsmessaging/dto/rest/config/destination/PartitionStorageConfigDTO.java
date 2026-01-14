package io.mapsmessaging.dto.rest.config.destination;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PartitionStorageConfigDTO extends StorageConfigDTO {

  @Schema(
      description = "Storage file name (typically derived from the logical name)"
  )
  private String fileName;

  @Schema(
      description = "Enable synchronous writes to disk",
      defaultValue = "false"
  )
  private boolean sync;

  @Schema(
      description = "Number of items per partition",
      defaultValue = "524288"
  )
  private int itemCount;

  @Schema(
      description = "Maximum number of events to hold",
      defaultValue = "-1"
  )
  private int capacity;

  @Schema(
      description = "Maximum size of a single partition in bytes",
      defaultValue = "4294967296"
  )
  private long maxPartitionSize;

  @Schema(
      description = "Polling interval (in seconds) for expired events",
      defaultValue = "1"
  )
  private int expiredEventPoll;

  @Schema(
      description = "Configuration to manage paused/idle messages stores when timeouts occur "
  )
  private DeferredConfigDTO deferredConfig;

  public PartitionStorageConfigDTO() {
    super("partition");
  }
}
