package io.mapsmessaging.dto.rest.config.destination;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MemoryStorageConfigDTO extends StorageConfigDTO {

  @Schema(
      description = "Polling interval (in seconds) for checking expired events",
      example = "1"
  )
  private int expiredEventPoll;

  @Schema(
      description = "Maximum number of messages that can be held in memory",
      example = "10000",
      defaultValue = "-1"
  )
  private int capacity;

  public MemoryStorageConfigDTO(){
    super("memory");
  }
}
